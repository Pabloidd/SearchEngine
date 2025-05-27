package searchengine.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;

@Repository
public interface SiteRepository extends org.springframework.data.jpa.repository.JpaRepository<Site, Integer> {
    Site findByUrl(String url);

    @Modifying
    @Transactional
    @Query(value = "ALTER TABLE site AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
}