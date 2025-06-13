package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Site;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с леммами
 */
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Optional<Lemma> findByLemmaAndSite(String lemma, Site site); // Найти лемму по тексту и сайту

    @Modifying
    @Transactional
    void deleteBySite(Site site); // Удалить леммы по сайту

    @Query("SELECT COUNT(l) FROM Lemma l WHERE l.site = :site")
    long countBySite(@Param("site") Site site); // Подсчитать леммы по сайту

    List<Lemma> findByLemmaInAndSite(List<String> lemmas, Site site); // Найти леммы по списку текстов и сайту
}