package com.liber.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Refresh token opaco — armazenado apenas como hash SHA-256.
 * O valor em texto puro so existe na resposta ao cliente; nunca persiste.
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_tokens_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_refresh_tokens_usuario", columnList = "usuario_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class RefreshToken extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotBlank
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    @ToString.Include
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    // Locking otimista — protege a rotacao de refresh tokens contra race: duas
    // requisicoes paralelas com o mesmo refresh competem na revogacao do atual
    // (UPDATE bumpa a version); a perdedora estoura OptimisticLockException
    // (tratada como 409 pelo GlobalExceptionHandler) em vez de gerar dois filhos
    // validos do mesmo pai (que mascararia o reuso real).
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public boolean isRevogado() {
        return revokedAt != null;
    }

    public boolean isExpirado(Instant agora) {
        return expiresAt.isBefore(agora);
    }

    public boolean isValido(Instant agora) {
        return !isRevogado() && !isExpirado(agora);
    }
}
