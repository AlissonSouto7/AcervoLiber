package com.liber.repository;

import com.liber.entity.Aluno;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlunoRepository extends JpaRepository<Aluno, Long> {

    Optional<Aluno> findByMatricula(String matricula);

    boolean existsByMatricula(String matricula);

    @Query("""
        SELECT a FROM Aluno a
        WHERE (:termo IS NULL OR :termo = ''
               OR LOWER(a.nome)      LIKE LOWER(CONCAT('%', :termo, '%'))
               OR LOWER(a.matricula) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR LOWER(a.turma)     LIKE LOWER(CONCAT('%', :termo, '%')))
        """)
    Page<Aluno> buscar(@Param("termo") String termo, Pageable pageable);

    /**
     * Carrega o aluno com SELECT FOR UPDATE — serializa registros de emprestimo
     * concorrentes para o mesmo aluno, garantindo a checagem do limite atomicamente.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Aluno a WHERE a.id = :id")
    Optional<Aluno> findByIdForUpdate(@Param("id") Long id);
}
