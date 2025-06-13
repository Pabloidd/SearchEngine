package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Сущность для хранения сайтов
 */
@Entity
@Table(name = "site")
@Getter
@Setter
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SiteStatus status; // Статус индексации

    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime; // Время последнего изменения статуса

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError; // Последняя ошибка

    @Column(nullable = false, length = 255)
    private String url; // URL сайта

    @Column(nullable = false, length = 255)
    private String name; // Название сайта
}