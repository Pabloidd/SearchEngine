package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;
import searchengine.model.SiteStatus;

import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    Site findByUrl(String url);

    @Modifying
    @Transactional
    @Query(value = "ALTER TABLE site AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();

    @Query("SELECT COUNT(s) FROM Site s WHERE s.status = :status")
    long countByStatus(@Param("status") SiteStatus status);

    List<Site> findByStatus(SiteStatus status);
}