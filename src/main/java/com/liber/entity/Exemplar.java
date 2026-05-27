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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Copia fisica de um livro com codigo proprio de tombamento. Empr e devolucao
 * sao por exemplar, nao pelo "livro generico" — isso permite a escola saber
 * exatamente qual copia esta com qual aluno (se o aluno perder, sabemos qual
 * etiqueta de patrimonio precisa ser baixada).
 *
 * <p>O {@code codigo} default e gerado por {@code exemplar_codigo_seq} no
 * formato {@code LIB-00001}; o bibliotecario pode editar pra bater com a
 * etiqueta fisica que a escola ja tem.
 */
@Entity
@Table(
    name = "exemplares",
    indexes = {
        @Index(name = "idx_exemplares_livro_id", columnList = "livro_id"),
        @Index(name = "idx_exemplares_situacao", columnList = "situacao")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Exemplar extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "livro_id", nullable = false)
    private Livro livro;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, unique = true, length = 50)
    @ToString.Include
    private String codigo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ToString.Include
    private SituacaoExemplar situacao;

    @Version
    @Column(nullable = false)
    private Long version;
}
