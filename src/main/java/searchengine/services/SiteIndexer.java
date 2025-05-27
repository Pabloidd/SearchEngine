package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.IndexingProperties;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Slf4j
public class SiteIndexer {
    private final String baseUrl;
    private final Site site;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final IndexingProperties properties;

    private final ConcurrentHashMap<String, Boolean> processedUrls = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    private final AtomicInteger totalDiscovered = new AtomicInteger(0);
    private final AtomicInteger successfullySaved = new AtomicInteger(0);
    private final AtomicInteger failedPages = new AtomicInteger(0);

    public SiteIndexer(String baseUrl, Site site,
                       PageRepository pageRepository,
                       SiteRepository siteRepository,
                       IndexingProperties properties) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.site = site;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.properties = properties;
        this.executor = Executors.newFixedThreadPool(properties.getMaxConcurrentPages());
    }

    public void indexSite() throws Exception {
        try {
            log.info("Starting indexing for: {}", site.getName());
            long startTime = System.currentTimeMillis();
            indexPage("", 0);
            waitForCompletion();

            if (isIndexingRunning()) {
                updateSiteStatus(SiteStatus.INDEXED, null);
                log.info("Completed indexing {}: {} pages in {} seconds",
                        site.getName(),
                        successfullySaved.get(),
                        (System.currentTimeMillis() - startTime) / 1000);
            }
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(2, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        }
    }

    private synchronized void indexPage(String path, int depth) {
        if (!isIndexingRunning() || shouldSkipIndexing(path, depth)) {
            return;
        }

        String fullUrl = normalizeUrl(baseUrl + path);
        if (!processedUrls.containsKey(fullUrl)) {
            processedUrls.put(fullUrl, false);
            activeTasks.incrementAndGet();
            totalDiscovered.incrementAndGet();
            executor.submit(() -> processPage(fullUrl, path, depth));
        }
    }

    private boolean shouldSkipIndexing(String path, int depth) {
        if (depth > getMaxAllowedDepthForSite()) {
            return true;
        }
        if (processedUrls.size() >= properties.getMaxPagesPerSite()) {
            return true;
        }
        for (String pattern : properties.getExcludePatterns()) {
            if (Pattern.matches(pattern, path)) {
                return true;
            }
        }
        return path.matches(".*(\\.(php|asp|aspx|jsp|cgi|do|action)|\\?|&|login|admin|auth|register).*");
    }

    private int getMaxAllowedDepthForSite() {
        String domain = getDomain(baseUrl);
        if (properties.getSiteDepths() != null && properties.getSiteDepths().containsKey(domain)) {
            return properties.getSiteDepths().get(domain);
        }
        return properties.getMaxDepth();
    }

    private void processPage(String fullUrl, String path, int depth) {
        try {
            processPageWithRetries(fullUrl, path, depth);
        } catch (Exception e) {
            log.warn("Failed to process {}: {}", fullUrl, e.getMessage());
            failedPages.incrementAndGet();
            processedUrls.remove(fullUrl);
        } finally {
            activeTasks.decrementAndGet();
        }
    }

    private void processPageWithRetries(String fullUrl, String path, int depth) throws Exception {
        for (int attempt = 1; attempt <= properties.getMaxRetries() && isIndexingRunning(); attempt++) {
            try {
                Connection.Response response = Jsoup.connect(fullUrl)
                        .userAgent(properties.getUserAgent())
                        .timeout(properties.getTimeoutMs())
                        .ignoreHttpErrors(true)
                        .followRedirects(true)
                        .execute();

                int delay = calculateDynamicDelay(attempt, fullUrl);
                Thread.sleep(delay);

                if (response.statusCode() != 200) {
                    if (attempt == properties.getMaxRetries()) {
                        log.warn("HTTP {} for {}", response.statusCode(), fullUrl);
                    }
                    continue;
                }

                Document doc = response.parse();
                savePage(response.statusCode(), path, doc.html());
                processNewLinks(doc, depth + 1);
                return;
            } catch (Exception e) {
                if (attempt == properties.getMaxRetries()) {
                    throw e;
                }
                Thread.sleep(2000L * attempt);
            }
        }
    }

    private int calculateDynamicDelay(int attempt, String url) {
        String domain = getDomain(url);
        int baseDelay = properties.getDelays().getSiteSpecific()
                .getOrDefault(domain, properties.getDelays().getDefault());
        return Math.max(properties.getMinDelayMs(), baseDelay * attempt);
    }

    private String getDomain(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            return domain != null ? domain.replaceFirst("^www\\.", "") : "";
        } catch (URISyntaxException e) {
            return "";
        }
    }

    private void processNewLinks(Document doc, int depth) {
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            if (!isIndexingRunning() || processedUrls.size() >= properties.getMaxPagesPerSite()) {
                break;
            }

            String href = link.attr("href");
            if (isValidLink(href)) {
                indexPage(normalizePath(href), depth);
            }
        }
    }

    private boolean isValidLink(String href) {
        if (href == null || href.isEmpty() ||
                href.startsWith("#") || href.startsWith("mailto:") ||
                href.startsWith("javascript:")) {
            return false;
        }
        return href.startsWith("/") || href.startsWith(baseUrl);
    }

    private synchronized void savePage(int statusCode, String path, String content) {
        try {
            Page page = new Page();
            page.setSite(site);
            page.setPath(path);
            page.setCode(statusCode);
            page.setContent(content);
            pageRepository.save(page);

            processedUrls.put(normalizeUrl(baseUrl + path), true);
            successfullySaved.incrementAndGet();

            if (successfullySaved.get() % 100 == 0) {
                updateSiteStatus(SiteStatus.INDEXING, null);
            }
        } catch (Exception e) {
            log.warn("Failed to save page {}: {}", path, e.getMessage());
            failedPages.incrementAndGet();
        }
    }

    private void waitForCompletion() throws InterruptedException {
        while (isIndexingRunning() && activeTasks.get() > 0) {
            Thread.sleep(2000);
            updateSiteStatus(SiteStatus.INDEXING, null);
        }
    }

    private void updateSiteStatus(SiteStatus status, String message) {
        site.setStatus(status);
        site.setLastError(message);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

    private boolean isIndexingRunning() {
        return site.getStatus() == SiteStatus.INDEXING;
    }

    private String normalizeUrl(String url) {
        return url.replaceAll("(?<!(http:|https:))//+", "/")
                .replaceAll("\\?.*$", "")
                .replaceAll("#.*$", "");
    }

    private String normalizePath(String path) {
        path = path.startsWith(baseUrl) ? path.substring(baseUrl.length()) : path;
        return "/" + path.replaceAll("^/+", "")
                .replaceAll("/+$", "")
                .replaceAll("/+", "/");
    }

    private String normalizeBaseUrl(String url) {
        return url.endsWith("/") ? url.substring(0, url.length()-1) : url;
    }
}