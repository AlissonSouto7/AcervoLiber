package com.liber.repository;

import com.liber.entity.Livro;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * Decremento atomico do estoque. Retorna 1 se conseguiu decrementar, 0 caso contrario
     * (livro inexistente ou sem exemplares disponiveis). Evita corrida em emprestimos concorrentes
     * sem precisar de lock pessimista — a propria clausula WHERE garante a invariante.
     */
    @Modifying
    @Query("""
        UPDATE Livro l
        SET l.quantidadeDisponivel = l.quantidadeDisponivel - 1,
            l.version = l.version + 1
        WHERE l.id = :id AND l.quantidadeDisponivel > 0
        """)
    int decrementarEstoque(@Param("id") Long id);

    /**
     * Incremento atomico no retorno. Guarda contra exceder o total de exemplares (defesa
     * em profundidade caso o estado divirja).
     */
    @Modifying
    @Query("""
        UPDATE Livro l
        SET l.quantidadeDisponivel = l.quantidadeDisponivel + 1,
            l.version = l.version + 1
        WHERE l.id = :id AND l.quantidadeDisponivel < l.quantidadeExemplares
        """)
    int incrementarEstoque(@Param("id") Long id);
}
