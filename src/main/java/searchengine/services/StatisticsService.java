package searchengine.services;

import searchengine.dto.statistics.StatisticsResponse;

/**
 * Интерфейс сервиса статистики
 */
public interface StatisticsService {
    /**
     * Получает статистику по индексации и сайтам
     * @return объект с данными статистики
     */
    StatisticsResponse getStatistics();
}