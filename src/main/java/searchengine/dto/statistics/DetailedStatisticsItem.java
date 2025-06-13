package searchengine.dto.statistics;

import lombok.Data;

/**
 * Детальная статистика по одному сайту
 */
@Data
public class DetailedStatisticsItem {
    private String url; // URL сайта
    private String name; // Название сайта
    private String status; // Статус индексации
    private long statusTime; // Время последнего изменения статуса
    private String error; // Последняя ошибка
    private int pages; // Количество страниц
    private int lemmas; // Количество лемм
}