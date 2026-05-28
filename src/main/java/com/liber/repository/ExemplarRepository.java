package com.liber.repository;

import com.liber.entity.Exemplar;
import com.liber.entity.SituacaoExemplar;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExemplarRepository extends JpaRepository<Exemplar, Long> {

    boolean existsByCodigo(String codigo);

    Optional<Exemplar> findByCodigo(String codigo);

    /** Exemplares de um livro, em ordem do codigo (apresentavel pro bibliotecario). */
    List<Exemplar> findByLivroIdOrderByCodigoAsc(Long livroId);

    long countByLivroId(Long livroId);

    long countByLivroIdAndSituacao(Long livroId, SituacaoExemplar situacao);

    /**
     * Exemplares DISPONIVEL de um livro, ordenados por id (FIFO de cadastro).
     * O service pega o primeiro com {@code .stream().findFirst()}.
     *
     * <p>Lock pessimista (FOR UPDATE) em todos os DISPONIVEL do livro — evita
     * que dois bibliotecarios/alunos peguem o mesmo exemplar simultaneamente.
     * Pra uma escola (poucos exemplares por livro), o overhead e desprezivel.
     *
     * <p>Antes era {@code Optional<Exemplar>} sem LIMIT — JPA estourava
     * {@code NonUniqueResultException} quando o livro tinha 2+ exemplares.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT e FROM Exemplar e
        WHERE e.livro.id = :livroId AND e.situacao = com.liber.entity.SituacaoExemplar.DISPONIVEL
        ORDER BY e.id ASC
        """)
    List<Exemplar> findDisponiveisForUpdate(@Param("livroId") Long livroId);

    /**
     * Proximo codigo padrao da sequence {@code exemplar_codigo_seq} criada em
     * V18, formatado como {@code LIB-00001}. O bibliotecario pode sobrescrever
     * com o codigo de etiqueta real da escola, mas o default acelera o cadastro.
     */
    @Query(value = "SELECT 'LIB-' || LPAD(nextval('exemplar_codigo_seq')::text, 5, '0')", nativeQuery = true)
    String proximoCodigoPadrao();
}
