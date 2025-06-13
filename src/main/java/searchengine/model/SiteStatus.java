package searchengine.model;

/**
 * Перечисление статусов сайта
 */
public enum SiteStatus {
    INDEXING, // В процессе индексации
    INDEXED,  // Успешно проиндексирован
    FAILED    // Ошибка индексации
}