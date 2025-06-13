package searchengine.dto.search;

import lombok.Data;

/**
 * Класс для представления одного результата поиска
 */
@Data
public class SearchResult {
    private String site; // URL сайта
    private String siteName; // Название сайта
    private String uri; // Путь страницы
    private String title; // Заголовок страницы
    private String snippet; // Сниппет с найденными словами
    private float relevance; // Релевантность результата
}