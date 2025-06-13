package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Сущность для хранения страниц
 */
@Entity
@Table(name = "page", indexes = {
        @jakarta.persistence.Index(name = "idx_path", columnList = "path")
})
@Getter
@Setter
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site; // Связанный сайт

    @Column(name = "path", nullable = false, length = 512)
    private String path; // Путь страницы

    @Column(nullable = false)
    private Integer code; // HTTP-код ответа

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content; // HTML-содержимое страницы
}