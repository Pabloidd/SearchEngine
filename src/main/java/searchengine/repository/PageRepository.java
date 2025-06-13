package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.Page;
import searchengine.model.Site;
import java.util.List;

/**
 * Репозиторий для работы со страницами
 */
public interface PageRepository extends JpaRepository<Page, Integer> {
    void deleteBySite(Site site); // Удалить страницы по сайту
    List<Page> findAllById(Iterable<Integer> ids); // Найти страницы по списку ID

    @Query("SELECT COUNT(p) FROM Page p WHERE p.site = :site")
    long countBySite(@Param("site") Site site); // Подсчитать страницы по сайту
}