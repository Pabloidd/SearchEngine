package searchengine.dto.statistics;

import lombok.Data;

/**
 * Общая статистика по всем сайтам
 */
@Data
public class TotalStatistics {
    private int sites; // Количество сайтов
    private int pages; // Количество страниц
    private int lemmas; // Количество лемм
    private boolean indexing; // Флаг активности индексации
}