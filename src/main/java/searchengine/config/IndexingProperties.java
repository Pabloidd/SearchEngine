package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing")
public class IndexingProperties {
    private String userAgent;
    private DelayProperties delays;
    private int maxPagesPerSite;
    private int timeoutMs;
    private int minDelayMs;
    private int maxRetries;
    private int maxDepth = 5;
    private int maxConcurrentPages;
    private List<String> excludePatterns;
    private Map<String, Integer> siteDepths; // Глубина для конкретных сайтов

    @Getter
    @Setter
    public static class DelayProperties {
        private int Default;
        private Map<String, Integer> siteSpecific;
    }
}