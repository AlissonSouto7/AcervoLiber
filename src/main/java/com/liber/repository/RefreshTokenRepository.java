package com.liber.repository;

import com.liber.entity.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Revoga todos os tokens ativos de um usuario (logout global / deteccao de reuso). */
    @Modifying
    @Query("""
        UPDATE RefreshToken rt
        SET rt.revokedAt = :agora
        WHERE rt.usuario.id = :usuarioId AND rt.revokedAt IS NULL
        """)
    int revogarTodosDoUsuario(@Param("usuarioId") Long usuarioId, @Param("agora") Instant agora);

    /** Remove tokens ja expirados — usado pelo job de limpeza. */
    @Modifying
    int deleteByExpiresAtBefore(Instant cutoff);
}
