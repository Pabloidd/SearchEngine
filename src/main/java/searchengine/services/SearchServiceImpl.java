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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaFinder lemmaFinder;

    @Override
    public SearchResponse search(String query, String site, int offset, int limit) {
        SearchResponse response = new SearchResponse();

        if (query == null || query.isEmpty()) {
            response.setResult(false);
            response.setError("Задан пустой поисковый запрос");
            return response;
        }

        List<Site> sitesToSearch = site != null ?
                Collections.singletonList(siteRepository.findByUrl(site)) :
                siteRepository.findAll();

        if (sitesToSearch.isEmpty() || (site != null && sitesToSearch.get(0) == null)) {
            response.setResult(false);
            response.setError("Указанный сайт не найден");
            return response;
        }

        Set<String> queryLemmas = lemmaFinder.getLemmaSet(query);
        if (queryLemmas.isEmpty()) {
            response.setResult(true);
            response.setCount(0);
            response.setData(Collections.emptyList());
            return response;
        }

        List<SearchResult> results = new ArrayList<>();
        for (Site siteEntity : sitesToSearch) {
            if (siteEntity.getStatus() != SiteStatus.INDEXED) {
                continue;
            }

            List<Lemma> foundLemmas = findRelevantLemmas(queryLemmas, siteEntity);
            if (foundLemmas.isEmpty()) {
                continue;
            }

            List<Page> relevantPages = findRelevantPages(foundLemmas);
            Map<Page, Float> pageRelevance = calculatePageRelevance(relevantPages, foundLemmas);

            results.addAll(createSearchResults(pageRelevance, siteEntity));
        }

        results.sort(Comparator.comparing(SearchResult::getRelevance).reversed());
        int total = results.size();
        List<SearchResult> paginatedResults = results.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        response.setResult(true);
        response.setCount(total);
        response.setData(paginatedResults);
        return response;
    }

    private List<Lemma> findRelevantLemmas(Set<String> queryLemmas, Site site) {
        return queryLemmas.stream()
                .map(lemma -> lemmaRepository.findByLemmaAndSite(lemma, site))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparingInt(Lemma::getFrequency))
                .collect(Collectors.toList());
    }

    private List<Page> findRelevantPages(List<Lemma> lemmas) {
        if (lemmas.isEmpty()) {
            return Collections.emptyList();
        }

        Lemma firstLemma = lemmas.get(0);
        List<Page> pages = indexRepository.findByLemma(firstLemma).stream()
                .map(Index::getPage)
                .collect(Collectors.toList());

        for (int i = 1; i < lemmas.size(); i++) {
            Lemma lemma = lemmas.get(i);
            List<Page> lemmaPages = indexRepository.findByLemma(lemma).stream()
                    .map(Index::getPage)
                    .collect(Collectors.toList());
            pages.retainAll(lemmaPages);
        }

        return pages;
    }

    private Map<Page, Float> calculatePageRelevance(List<Page> pages, List<Lemma> lemmas) {
        if (pages.isEmpty()) {
            return Collections.emptyMap();
        }

        float maxRank = 0;
        Map<Page, Float> pageRank = new HashMap<>();

        for (Page page : pages) {
            float rank = 0;
            for (Lemma lemma : lemmas) {
                Index index = indexRepository.findByPageAndLemma(page, lemma);
                if (index != null) {
                    rank += index.getRank();
                }
            }
            pageRank.put(page, rank);
            if (rank > maxRank) {
                maxRank = rank;
            }
        }

        if (maxRank > 0) {
            for (Map.Entry<Page, Float> entry : pageRank.entrySet()) {
                entry.setValue(entry.getValue() / maxRank);
            }
        }

        return pageRank;
    }

    private List<SearchResult> createSearchResults(Map<Page, Float> pageRelevance, Site site) {
        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<Page, Float> entry : pageRelevance.entrySet()) {
            Page page = entry.getKey();
            Document doc = Jsoup.parse(page.getContent());
            String title = doc.title();
            String snippet = createSnippet(doc.text());

            SearchResult result = new SearchResult();
            result.setSite(site.getUrl());
            result.setSiteName(site.getName());
            result.setUri(page.getPath());
            result.setTitle(title);
            result.setSnippet(snippet);
            result.setRelevance(entry.getValue());

            results.add(result);
        }
        return results;
    }

    private String createSnippet(String text) {
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }
}