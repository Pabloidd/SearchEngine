package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "page", indexes = {
        @Index(name = "idx_path", columnList = "path")
})
@Getter
@Setter
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "path", nullable = false, length = 512)
    private String path;

    @Column(nullable = false)
    private Integer code;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;
}