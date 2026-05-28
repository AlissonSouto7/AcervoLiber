package com.liber.repository;

import com.liber.entity.Reserva;
import com.liber.entity.StatusReserva;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Fila de reservas pendentes para o bibliotecario (mais antigas primeiro), paginada. */
    @EntityGraph(attributePaths = {"livro", "aluno"})
    Page<Reserva> findByStatusOrderByDataReservaAsc(StatusReserva status, Pageable pageable);

    /** Reservas de um aluno (mais recentes primeiro). */
    @EntityGraph(attributePaths = {"livro", "aluno"})
    Page<Reserva> findByAlunoIdOrderByDataReservaDesc(Long alunoId, Pageable pageable);

    /** Carrega uma reserva garantindo que pertence ao aluno informado. */
    Optional<Reserva> findByIdAndAlunoId(Long id, Long alunoId);

    long countByAlunoIdAndStatus(Long alunoId, StatusReserva status);

    /** Reservas de um livro num dado status — usado para recalcular o estoque disponivel. */
    long countByLivroIdAndStatus(Long livroId, StatusReserva status);

    boolean existsByAlunoIdAndLivroIdAndStatus(Long alunoId, Long livroId, StatusReserva status);

    /** Reservas pendentes que ja passaram da validade — usado pelo job de expiracao. */
    List<Reserva> findByStatusAndDataExpiracaoBefore(StatusReserva status, LocalDate data);

    /** Existe alguma reserva (qualquer status) para o aluno? Usado em remocao. */
    boolean existsByAlunoId(Long alunoId);

    /**
     * Desassocia o emprestimo das reservas que o referenciam (seta para NULL).
     * Usado antes de remover um emprestimo do historico — sem isso, a FK de
     * reservas.emprestimo_id viola.
     */
    @Modifying
    @Query("UPDATE Reserva r SET r.emprestimo = null WHERE r.emprestimo.id = :emprestimoId")
    int desassociarDoEmprestimo(@Param("emprestimoId") Long emprestimoId);

    /** Apaga todas as reservas de um aluno — usado no cascade de remover aluno. */
    @Modifying
    @Query("DELETE FROM Reserva r WHERE r.aluno.id = :alunoId")
    int deleteAllByAlunoId(@Param("alunoId") Long alunoId);
}
