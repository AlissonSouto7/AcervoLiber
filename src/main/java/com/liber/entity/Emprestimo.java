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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
    name = "emprestimos",
    indexes = {
        @Index(name = "idx_emprestimos_situacao", columnList = "situacao"),
        @Index(name = "idx_emprestimos_data_devolucao_prevista", columnList = "data_devolucao_prevista"),
        @Index(name = "idx_emprestimos_aluno_situacao", columnList = "aluno_id,situacao"),
        @Index(name = "idx_emprestimos_exemplar", columnList = "exemplar_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Emprestimo extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    /**
     * Exemplar fisico emprestado. Saber a copia exata (e nao apenas o titulo)
     * permite responsabilizar pelo extravio: se o aluno perder o livro, a escola
     * sabe que foi o exemplar de codigo X que ficou faltando do acervo.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exemplar_id", nullable = false)
    private Exemplar exemplar;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @NotNull
    @Column(name = "data_emprestimo", nullable = false)
    @ToString.Include
    private LocalDate dataEmprestimo;

    @NotNull
    @Min(1)
    @Column(name = "prazo_dias", nullable = false)
    private Integer prazoDias;

    @NotNull
    @Column(name = "data_devolucao_prevista", nullable = false)
    @ToString.Include
    private LocalDate dataDevolucaoPrevista;

    @Column(name = "data_devolucao_efetiva")
    private LocalDate dataDevolucaoEfetiva;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ToString.Include
    private SituacaoEmprestimo situacao;

    /**
     * Numero de renovacoes ja aplicadas a este emprestimo (0 = nunca foi renovado).
     * O limite e configuravel via {@code app.emprestimo.max-renovacoes} (default 2).
     */
    @NotNull
    @Min(0)
    @Column(name = "renovacoes", nullable = false)
    @Builder.Default
    private Integer renovacoes = 0;

    @Version
    @Column(nullable = false)
    private Long version;
}
