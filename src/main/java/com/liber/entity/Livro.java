package com.liber.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
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

@Entity
@Table(
    name = "livros",
    indexes = {
        @Index(name = "idx_livros_titulo", columnList = "titulo"),
        @Index(name = "idx_livros_autor", columnList = "autor")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Livro extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    @ToString.Include
    private String titulo;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String autor;

    @Size(max = 20)
    @Column(length = 20, unique = true)
    private String isbn;

    private Integer ano;

    @NotNull
    @Min(0)
    @Column(name = "quantidade_exemplares", nullable = false)
    private Integer quantidadeExemplares;

    @NotNull
    @Min(0)
    @Column(name = "quantidade_disponivel", nullable = false)
    private Integer quantidadeDisponivel;

    /**
     * URL da capa do livro. Pode ser uma URL externa (Google Books / Open Library,
     * resolvida automaticamente) ou a URL do endpoint que serve a capa enviada
     * manualmente. Nula quando nenhuma fonte tem capa.
     */
    @Size(max = 500)
    @Column(name = "capa_url", length = 500)
    private String capaUrl;

    /**
     * Quando true, a capa foi enviada manualmente pelo admin/bibliotecario e a
     * resolucao automatica nao deve sobrescreve-la.
     */
    @Column(name = "capa_manual", nullable = false)
    private boolean capaManual;

    @Version
    @Column(nullable = false)
    private Long version;
}
