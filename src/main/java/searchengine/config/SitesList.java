package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings")// значение prefix — это название ключа конфигурации, внутри которого лежит список сайтов Аннотации приводят к автоматической инициализации объекта этого класса данными из файла application.yaml.
public class SitesList {
    private List<Site> sites;
}
