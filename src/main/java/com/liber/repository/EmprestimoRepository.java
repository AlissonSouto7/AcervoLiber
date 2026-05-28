package com.liber.repository;

import com.liber.dto.LivroRankingDTO;
import com.liber.entity.Emprestimo;
import com.liber.entity.SituacaoEmprestimo;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long> {

    long countBySituacao(SituacaoEmprestimo situacao);

    long countByAlunoIdAndSituacao(Long alunoId, SituacaoEmprestimo situacao);

    /** Apaga todos os emprestimos de um aluno — usado no cascade de remover aluno. */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Emprestimo e WHERE e.aluno.id = :alunoId")
    int deleteAllByAlunoId(@org.springframework.data.repository.query.Param("alunoId") Long alunoId);

    boolean existsByExemplarId(Long exemplarId);

    /** Indica se ha qualquer emprestimo (ativo ou historico) com algum exemplar do livro. */
    @Query("""
        SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
        FROM Emprestimo e WHERE e.exemplar.livro.id = :livroId
        """)
    boolean existsByLivroId(@Param("livroId") Long livroId);

    boolean existsByAlunoId(Long alunoId);

    @EntityGraph(attributePaths = {"exemplar.livro", "aluno"})
    List<Emprestimo> findBySituacaoOrderByDataDevolucaoPrevistaAsc(SituacaoEmprestimo situacao);

    @EntityGraph(attributePaths = {"exemplar.livro", "aluno"})
    Page<Emprestimo> findAllByOrderByDataEmprestimoDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"exemplar.livro", "aluno"})
    Page<Emprestimo> findByAlunoIdOrderByDataEmprestimoDesc(Long alunoId, Pageable pageable);

    @Query("""
        SELECT COUNT(e) FROM Emprestimo e
        WHERE e.situacao = com.liber.entity.SituacaoEmprestimo.ATIVO
          AND e.dataDevolucaoPrevista < :hoje
        """)
    long countAtrasados(@Param("hoje") LocalDate hoje);

    @Query("""
        SELECT COUNT(e) FROM Emprestimo e
        WHERE e.aluno.id = :alunoId
          AND e.situacao = com.liber.entity.SituacaoEmprestimo.ATIVO
          AND e.dataDevolucaoPrevista < :hoje
        """)
    long countAtrasadosByAluno(@Param("alunoId") Long alunoId, @Param("hoje") LocalDate hoje);

    @Query("""
        SELECT new com.liber.dto.LivroRankingDTO(
            l.id, l.titulo, l.autor, COUNT(e)
        )
        FROM Emprestimo e JOIN e.exemplar ex JOIN ex.livro l
        GROUP BY l.id, l.titulo, l.autor
        ORDER BY COUNT(e) DESC, l.titulo ASC
        """)
    List<LivroRankingDTO> rankingLivrosMaisEmprestados(Pageable pageable);
}
