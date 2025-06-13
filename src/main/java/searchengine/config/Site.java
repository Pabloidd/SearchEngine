package searchengine.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Класс для представления сайта в конфигурации
 */
@Setter
@Getter
public class Site {
    private String url;  // URL сайта
    private String name; // Название сайта
}