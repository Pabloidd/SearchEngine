package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.*;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        // Создаем общую статистику
        TotalStatistics total = new TotalStatistics();
        total.setSites((int) siteRepository.count());
        total.setPages((int) pageRepository.count());
        total.setLemmas((int) lemmaRepository.count());
        total.setIndexing(isIndexingActive());

        // Создаем детализированную статистику по каждому сайту
        List<DetailedStatisticsItem> detailed = siteRepository.findAll().stream()
                .map(this::convertToDetailedStatistics)
                .collect(Collectors.toList());

        // Формируем ответ
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);

        StatisticsResponse response = new StatisticsResponse();
        response.setResult(true);
        response.setStatistics(data);
        return response;
    }

    /**
     * Конвертирует данные сайта в объект детализированной статистики
     */
    private DetailedStatisticsItem convertToDetailedStatistics(Site site) {
        DetailedStatisticsItem item = new DetailedStatisticsItem();
        item.setName(site.getName());
        item.setUrl(site.getUrl());
        item.setStatus(site.getStatus().toString());
        item.setStatusTime(site.getStatusTime().toEpochSecond(java.time.ZoneOffset.UTC));
        item.setError(site.getLastError());
        item.setPages((int) pageRepository.countBySite(site));
        item.setLemmas((int) lemmaRepository.countBySite(site));
        return item;
    }

    /**
     * Проверяет, идет ли в данный момент индексация
     */
    private boolean isIndexingActive() {
        return siteRepository.countByStatus(SiteStatus.INDEXING) > 0;
    }
}