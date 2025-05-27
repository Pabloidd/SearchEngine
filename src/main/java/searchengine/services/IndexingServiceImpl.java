package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.IndexingProperties;
import searchengine.config.SitesList;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexingProperties indexingProperties;

    private ExecutorService siteExecutor;
    private List<Future<?>> siteFutures;
    private volatile boolean isIndexingRunning = false;

    @Override
    @Transactional
    public synchronized boolean startIndexing() {
        if (isIndexingRunning) {
            log.info("Indexing already running");
            return false;
        }

        isIndexingRunning = true;
        int poolSize = Math.min(32, sitesList.getSites().size() * 4);
        siteExecutor = Executors.newFixedThreadPool(poolSize);
        siteFutures = new ArrayList<>();

        log.info("Starting indexing for {} sites", sitesList.getSites().size());

        for (searchengine.config.Site configSite : sitesList.getSites()) {
            siteFutures.add(siteExecutor.submit(() -> {
                try {
                    Site site = prepareSiteForIndexing(configSite);
                    new SiteIndexer(
                            configSite.getUrl(),
                            site,
                            pageRepository,
                            siteRepository,
                            indexingProperties
                    ).indexSite();
                } catch (Exception e) {
                    log.warn("Indexing failed for {}: {}", configSite.getUrl(), e.getMessage());
                    updateSiteStatus(configSite.getUrl(), SiteStatus.FAILED, e.getMessage());
                }
            }));
        }

        new Thread(this::monitorSiteIndexing).start();
        return true;
    }

    @Override
    public synchronized boolean stopIndexing() {
        if (!isIndexingRunning) {
            log.info("No active indexing to stop");
            return false;
        }

        log.info("Stopping indexing");
        isIndexingRunning = false;
        siteFutures.forEach(future -> future.cancel(true));
        siteExecutor.shutdownNow();

        try {
            if (!siteExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                siteExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            siteExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        updateAllSitesStatus(SiteStatus.FAILED, "Indexing stopped by user");
        return true;
    }

    private Site prepareSiteForIndexing(searchengine.config.Site configSite) {
        Site existingSite = siteRepository.findByUrl(configSite.getUrl());
        if (existingSite != null) {
            pageRepository.deleteBySite(existingSite);
            siteRepository.delete(existingSite);
            siteRepository.resetAutoIncrement();
        }

        Site site = new Site();
        site.setUrl(configSite.getUrl());
        site.setName(configSite.getName());
        site.setStatus(SiteStatus.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        return siteRepository.save(site);
    }

    private void monitorSiteIndexing() {
        while (isIndexingRunning && !allTasksCompleted()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        isIndexingRunning = false;
        siteExecutor.shutdown();
    }

    private boolean allTasksCompleted() {
        for (Future<?> future : siteFutures) {
            if (!future.isDone()) {
                return false;
            }
        }
        return true;
    }

    private void updateAllSitesStatus(SiteStatus status, String errorMessage) {
        siteRepository.findAll().forEach(site -> {
            if (site.getStatus() == SiteStatus.INDEXING) {
                site.setStatus(status);
                if (status == SiteStatus.FAILED) {
                    site.setLastError(errorMessage);
                }
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);
            }
        });
    }

    private void updateSiteStatus(String url, SiteStatus status, String error) {
        Site site = siteRepository.findByUrl(url);
        if (site != null) {
            site.setStatus(status);
            site.setLastError(error);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        }
    }
}