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

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);

    @Modifying
    @Transactional
    void deleteBySite(Site site);

    @Query("SELECT COUNT(l) FROM Lemma l WHERE l.site = :site")
    long countBySite(@Param("site") Site site);

    List<Lemma> findByLemmaInAndSite(List<String> lemmas, Site site);
}