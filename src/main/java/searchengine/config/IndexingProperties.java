package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Класс для хранения настроек индексации из конфигурационного файла
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing")
public class IndexingProperties {
    private String userAgent;                      // User-Agent для запросов
    private DelayProperties delays;               // Настройки задержек
    private int maxPagesPerSite;                  // Макс. количество страниц на сайт
    private int timeoutMs;                        // Таймаут запросов в мс
    private int minDelayMs;                       // Минимальная задержка между запросами
    private int maxRetries;                      // Макс. количество попыток повторного запроса
    private int maxDepth = 5;                     // Макс. глубина индексации по умолчанию
    private int maxConcurrentPages;               // Макс. параллельных страниц для обработки
    private List<String> excludePatterns;         // Паттерны для исключения URL
    private Map<String, Integer> siteDepths;      // Глубина индексации для конкретных сайтов

    /**
     * Вложенный класс для настроек задержек
     */
    @Getter
    @Setter
    public static class DelayProperties {
        private int Default;                      // Задержка по умолчанию
        private Map<String, Integer> siteSpecific; // Задержки для конкретных сайтов
    }
}