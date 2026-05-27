package com.liber.repository;

import com.liber.entity.Livro;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LivroRepository extends JpaRepository<Livro, Long> {

    Optional<Livro> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    /** Livros que tem ISBN mas ainda nao tiveram a capa resolvida — alvo do backfill. */
    List<Livro> findByIsbnIsNotNullAndCapaUrlIsNull();

    /** Livros que tem ISBN mas ainda nao tem sinopse — alvo do backfill de sinopse. */
    List<Livro> findByIsbnIsNotNullAndSinopseIsNull();

    @Query("""
        SELECT l FROM Livro l
        WHERE (:termo IS NULL OR :termo = ''
               OR LOWER(l.titulo) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR LOWER(l.autor)  LIKE LOWER(CONCAT('%', :termo, '%'))
               OR l.isbn = :termo)
        """)
    Page<Livro> buscar(@Param("termo") String termo, Pageable pageable);
}
