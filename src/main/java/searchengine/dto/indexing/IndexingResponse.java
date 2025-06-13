package searchengine.dto.indexing;

import lombok.Data;

/**
 * Ответ API для операций индексации
 */
@Data
public class IndexingResponse {
    private boolean result; // Результат операции
    private String error; // Сообщение об ошибке

    public IndexingResponse(boolean result) {
        this.result = result;
    }

    public IndexingResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}