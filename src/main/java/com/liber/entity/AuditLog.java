package com.liber.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Registro IMUTAVEL de um evento de seguranca (trilha de auditoria).
 * Nao estende AuditableEntity — eventos sao imutaveis, so importa quando ocorreram.
 *
 * <p>Sem {@code @Setter}: a unica forma de criar e via {@code @Builder} (apenas em
 * {@code AuditService.registrar}). Tentativas futuras de mutar um registro existente
 * via setter sao bloqueadas em compile time. Defesa em profundidade contra adminis-
 * tradores comprometidos que tentem apagar/editar rastros via codigo da aplicacao.
 */
@Entity
@Table(
    name = "audit_log",
    indexes = {
        @Index(name = "idx_audit_log_evento", columnList = "evento"),
        @Index(name = "idx_audit_log_usuario_email", columnList = "usuario_email"),
        @Index(name = "idx_audit_log_ator_email", columnList = "ator_email"),
        @Index(name = "idx_audit_log_ocorrido_em", columnList = "ocorrido_em")
    }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @ToString.Include
    private EventoAuditoria evento;

    /** Email do ALVO do evento — texto livre (login falho pode citar um email inexistente). */
    @Column(name = "usuario_email", length = 150)
    @ToString.Include
    private String usuarioEmail;

    /**
     * Email de quem EXECUTOU a acao (capturado do SecurityContext). Nullable: eventos
     * sem ator autenticado (LOGIN_FALHA, REFRESH_REUSO) ficam NULL. Importante para
     * forense: "quem desativou o admin?" só responde com este campo.
     */
    @Column(name = "ator_email", length = 150)
    @ToString.Include
    private String atorEmail;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(length = 500)
    private String detalhe;

    @CreationTimestamp
    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    @ToString.Include
    private Instant ocorridoEm;
}
