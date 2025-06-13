package searchengine.services;

import searchengine.dto.search.SearchResponse;

/**
 * Интерфейс сервиса поиска
 */
public interface SearchService {
    /**
     * Выполняет поиск по запросу
     * @param query поисковый запрос
     * @param site URL сайта для ограничения поиска (может быть null)
     * @param offset смещение для пагинации
     * @param limit количество результатов на странице
     * @return ответ с результатами поиска
     */
    SearchResponse search(String query, String site, int offset, int limit);
}