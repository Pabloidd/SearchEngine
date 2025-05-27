package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.Page;
import searchengine.model.Site;

public interface PageRepository extends JpaRepository<Page, Integer> {
    void deleteBySite(Site site);

    @Query("SELECT COUNT(p) FROM Page p WHERE p.site = :site")
    long countBySite(@Param("site") Site site);
}