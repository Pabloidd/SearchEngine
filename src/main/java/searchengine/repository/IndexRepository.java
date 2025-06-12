package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    List<Index> findByLemma(Lemma lemma);
    List<Index> findByPage(Page page);
    Index findByPageAndLemma(Page page, Lemma lemma);

    @Query("SELECT i FROM Index i WHERE i.page IN :pages AND i.lemma IN :lemmas")
    List<Index> findByPagesAndLemmas(@Param("pages") List<Page> pages,
                                     @Param("lemmas") List<Lemma> lemmas);
}