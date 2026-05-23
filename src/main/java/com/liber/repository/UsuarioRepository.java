package com.liber.repository;

import com.liber.entity.Role;
import com.liber.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    /**
     * Conta usuarios ativos de um role, excluindo um id (geralmente o que esta
     * sendo desativado). Usado para impedir que o ULTIMO admin desative a si
     * mesmo, deixando o sistema sem nenhum administrador.
     */
    long countByRoleAndAtivoIsTrueAndIdNot(Role role, Long id);

    /** Verifica se um aluno ja possui acesso ao sistema. */
    boolean existsByAlunoId(Long alunoId);

    /** Resolve o usuario de um aluno pela matricula — usado no login do aluno. */
    Optional<Usuario> findByAlunoMatricula(String matricula);
}
