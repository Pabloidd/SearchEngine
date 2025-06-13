package searchengine.dto.statistics;

import lombok.Data;

/**
 * Ответ API для статистики
 */
@Data
public class StatisticsResponse {
    private boolean result; // Результат операции
    private StatisticsData statistics; // Данные статистики
}