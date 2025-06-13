package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Сущность для хранения лемм
 */
@Entity
@Table(name = "lemma")
@Getter
@Setter
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site; // Связанный сайт

    @Column(nullable = false)
    private String lemma; // Текст леммы

    @Column(nullable = false)
    private Integer frequency; // Частота встречаемости
}