package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Сущность для хранения индексов (связей между страницами и леммами)
 */
@Entity
@Table(name = "`index`")
@Getter
@Setter
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page; // Связанная страница

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma; // Связанная лемма

    @Column(name = "`rank`", nullable = false)
    private Float rank; // Ранг (вес) связи
}