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

    long countByLivroIdAndSituacao(Long livroId, SituacaoEmprestimo situacao);

    boolean existsByLivroId(Long livroId);

    boolean existsByAlunoId(Long alunoId);

    @EntityGraph(attributePaths = {"livro", "aluno"})
    List<Emprestimo> findBySituacaoOrderByDataDevolucaoPrevistaAsc(SituacaoEmprestimo situacao);

    @EntityGraph(attributePaths = {"livro", "aluno"})
    Page<Emprestimo> findAllByOrderByDataEmprestimoDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"livro", "aluno"})
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
        FROM Emprestimo e JOIN e.livro l
        GROUP BY l.id, l.titulo, l.autor
        ORDER BY COUNT(e) DESC, l.titulo ASC
        """)
    List<LivroRankingDTO> rankingLivrosMaisEmprestados(Pageable pageable);
}
