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
}
