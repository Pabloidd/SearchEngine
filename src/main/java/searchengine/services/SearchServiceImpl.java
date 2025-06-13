package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResult;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Реализация сервиса поиска
 */
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaFinder lemmaFinder;

    @Override
    public SearchResponse search(String query, String siteUrl, int offset, int limit) {
        SearchResponse response = new SearchResponse();

        // Проверка на пустой запрос
        if (query == null || query.trim().isEmpty()) {
            response.setResult(false);
            response.setError("Задан пустой поисковый запрос");
            return response;
        }

        try {
            // Получаем сайт(ы) для поиска
            List<Site> sitesToSearch = getSitesToSearch(siteUrl);
            if (sitesToSearch.isEmpty()) {
                response.setResult(false);
                response.setError(siteUrl != null ?
                        "Указанный сайт не проиндексирован" :
                        "Нет проиндексированных сайтов");
                return response;
            }

            // Обрабатываем поисковый запрос
            Set<String> queryLemmas = lemmaFinder.getLemmaSet(query);
            if (queryLemmas.isEmpty()) {
                return new SearchResponse(true, 0, null, Collections.emptyList());
            }

            // Выполняем поиск по сайтам
            List<SearchResult> allResults = new ArrayList<>();
            for (Site site : sitesToSearch) {
                allResults.addAll(searchInSite(site, queryLemmas, query));
            }

            // Фильтрация и сортировка результатов
            List<SearchResult> filteredResults = filterAndSortResults(allResults);

            // Применяем пагинацию
            int total = filteredResults.size();
            List<SearchResult> paginatedResults = filteredResults.stream()
                    .skip(offset)
                    .limit(limit)
                    .collect(Collectors.toList());

            response.setResult(true);
            response.setCount(total);
            response.setData(paginatedResults);

        } catch (Exception e) {
            response.setResult(false);
            response.setError("Ошибка при выполнении поиска: " + e.getMessage());
        }

        return response;
    }

    private List<Site> getSitesToSearch(String siteUrl) {
        if (siteUrl != null) {
            Site site = siteRepository.findByUrl(siteUrl);
            return site != null && site.getStatus() == SiteStatus.INDEXED ?
                    Collections.singletonList(site) :
                    Collections.emptyList();
        }
        return siteRepository.findByStatus(SiteStatus.INDEXED);
    }

    private List<SearchResult> searchInSite(Site site, Set<String> queryLemmas, String originalQuery) {
        List<Lemma> foundLemmas = getFilteredLemmas(site, queryLemmas);
        if (foundLemmas.isEmpty()) {
            return Collections.emptyList();
        }

        List<Page> pages = findPagesContainingAllLemmas(site, foundLemmas);
        if (pages.isEmpty()) {
            return Collections.emptyList();
        }

        return createSearchResults(pages, foundLemmas, site, originalQuery);
    }

    private List<Lemma> getFilteredLemmas(Site site, Set<String> queryLemmas) {
        long totalPages = pageRepository.countBySite(site);
        double frequencyThreshold = totalPages * 0.8;

        return queryLemmas.stream()
                .map(lemma -> lemmaRepository.findByLemmaAndSite(lemma, site))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(lemma -> lemma.getFrequency() <= frequencyThreshold)
                .sorted(Comparator.comparingInt(Lemma::getFrequency))
                .collect(Collectors.toList());
    }

    private List<Page> findPagesContainingAllLemmas(Site site, List<Lemma> lemmas) {
        if (lemmas.isEmpty()) {
            return Collections.emptyList();
        }

        Lemma rarestLemma = lemmas.get(0);
        Set<Integer> pageIds = indexRepository.findByLemma(rarestLemma).stream()
                .map(index -> index.getPage().getId())
                .collect(Collectors.toSet());

        for (int i = 1; i < lemmas.size() && !pageIds.isEmpty(); i++) {
            Lemma lemma = lemmas.get(i);
            Set<Integer> currentPageIds = indexRepository.findByLemma(lemma).stream()
                    .map(index -> index.getPage().getId())
                    .collect(Collectors.toSet());
            pageIds.retainAll(currentPageIds);
        }

        return pageIds.isEmpty() ?
                Collections.emptyList() :
                pageRepository.findAllById(pageIds);
    }

    private List<SearchResult> createSearchResults(List<Page> pages,
                                                   List<Lemma> lemmas,
                                                   Site site,
                                                   String originalQuery) {
        Map<Page, Float> pageRelevanceMap = calculateTfIdfRelevance(pages, lemmas, site);

        return pages.stream()
                .map(page -> createSearchResult(page, site, pageRelevanceMap.get(page), originalQuery))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<Page, Float> calculateTfIdfRelevance(List<Page> pages, List<Lemma> lemmas, Site site) {
        long totalPages = pageRepository.countBySite(site);
        Map<Page, Float> relevanceMap = new HashMap<>();
        float maxRelevance = 0;

        for (Page page : pages) {
            float relevance = 0;

            for (Lemma lemma : lemmas) {
                Optional<Index> indexOpt = indexRepository.findByPageAndLemma(page, lemma);
                if (indexOpt.isPresent()) {
                    float tf = indexOpt.get().getRank();
                    float idf = (float) Math.log((double) totalPages / lemma.getFrequency());
                    relevance += tf * idf;
                }
            }

            relevanceMap.put(page, relevance);
            if (relevance > maxRelevance) {
                maxRelevance = relevance;
            }
        }

        if (maxRelevance > 0) {
            float finalMaxRelevance = maxRelevance;
            relevanceMap.replaceAll((page, rel) -> rel / finalMaxRelevance);
        }

        return relevanceMap;
    }

    private SearchResult createSearchResult(Page page, Site site, float relevance, String originalQuery) {
        if (page.getPath().contains("/admin/") || page.getPath().contains("/api/")) {
            return null;
        }

        String content = page.getContent();
        SearchResult result = new SearchResult();
        result.setSite(site.getUrl());
        result.setSiteName(site.getName());
        result.setUri(page.getPath());
        result.setTitle(extractTitle(content));
        result.setSnippet(generateSnippet(content, originalQuery));
        result.setRelevance(relevance);

        return result;
    }

    private String extractTitle(String html) {
        try {
            Document doc = Jsoup.parse(html);
            String title = doc.title();
            return title != null && !title.isEmpty() ? title : "Без названия";
        } catch (Exception e) {
            return "Без названия";
        }
    }

    private String generateSnippet(String html, String originalQuery) {
        String cleanText = Jsoup.parse(html).text();
        if (cleanText.length() > 1000) {
            cleanText = cleanText.substring(0, 1000) + "...";
        }

        String lowerQuery = originalQuery.toLowerCase();
        String lowerText = cleanText.toLowerCase();
        int queryPos = lowerText.indexOf(lowerQuery);

        if (queryPos >= 0) {
            int start = Math.max(0, queryPos - 50);
            int end = Math.min(cleanText.length(), queryPos + originalQuery.length() + 50);
            String snippet = cleanText.substring(start, end);

            return snippet.replaceAll("(?i)(" + Pattern.quote(originalQuery) + ")", "<b>$1</b>");
        }

        String[] words = cleanText.split("\\s+");
        StringBuilder snippet = new StringBuilder();
        int addedWords = 0;

        for (String word : words) {
            if (addedWords >= 30) break;

            String cleanWord = word.toLowerCase().replaceAll("[^а-яё]", "");
            if (lemmaFinder.getLemmaSet(word).stream().anyMatch(lemma ->
                    originalQuery.toLowerCase().contains(lemma.toLowerCase()))) {
                snippet.append("<b>").append(word).append("</b> ");
            } else {
                snippet.append(word).append(" ");
            }
            addedWords++;
        }

        return snippet.toString().trim() + (addedWords < words.length ? "..." : "");
    }

    private List<SearchResult> filterAndSortResults(List<SearchResult> results) {
        Map<String, SearchResult> uniqueResults = new LinkedHashMap<>();
        for (SearchResult result : results) {
            if (result != null) {
                uniqueResults.putIfAbsent(result.getUri(), result);
            }
        }

        return uniqueResults.values().stream()
                .sorted(Comparator.comparingDouble(SearchResult::getRelevance).reversed())
                .collect(Collectors.toList());
    }
}