package searchengine.dto.search;

import lombok.Data;
import java.util.List;

/**
 * Ответ API для поисковых запросов
 */
@Data
public class SearchResponse {
    private boolean result; // Результат операции
    private int count; // Общее количество результатов
    private String error; // Сообщение об ошибке
    private List<SearchResult> data; // Список результатов поиска

    public SearchResponse() {}

    public SearchResponse(boolean result, int count, String error, List<SearchResult> data) {
        this.result = result;
        this.count = count;
        this.error = error;
        this.data = data;
    }
}