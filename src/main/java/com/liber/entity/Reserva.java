package com.liber.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Reserva de um livro feita por um aluno. Enquanto PENDENTE, segura um exemplar
 * (o estoque disponivel do livro fica decrementado).
 */
@Entity
@Table(
    name = "reservas",
    indexes = {
        @Index(name = "idx_reservas_status", columnList = "status"),
        @Index(name = "idx_reservas_aluno", columnList = "aluno_id"),
        @Index(name = "idx_reservas_livro", columnList = "livro_id"),
        @Index(name = "idx_reservas_exemplar", columnList = "exemplar_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Reserva extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "livro_id", nullable = false)
    private Livro livro;

    /**
     * Exemplar especifico segurado pela reserva. Preenchido ja na criacao (o
     * primeiro DISPONIVEL e tomado pra evitar over-booking de copias). Na
     * confirmacao, o bibliotecario pode trocar antes de finalizar.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exemplar_id")
    private Exemplar exemplar;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ToString.Include
    private StatusReserva status;

    @NotNull
    @Column(name = "data_reserva", nullable = false)
    private LocalDate dataReserva;

    @NotNull
    @Column(name = "data_expiracao", nullable = false)
    private LocalDate dataExpiracao;

    /** Momento em que a reserva foi resolvida (confirmada/recusada/cancelada/expirada). */
    @Column(name = "data_resolucao")
    private Instant dataResolucao;

    /** Emprestimo gerado quando a reserva e confirmada. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emprestimo_id")
    private Emprestimo emprestimo;

    @Version
    @Column(nullable = false)
    private Long version;
}
