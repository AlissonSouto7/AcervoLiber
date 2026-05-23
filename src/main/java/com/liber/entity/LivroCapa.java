package com.liber.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bytes da capa enviada manualmente para um livro.
 *
 * <p>Tabela separada de {@code livros} de proposito: assim a listagem de livros
 * nao carrega o binario da imagem em toda consulta. O id e o proprio
 * {@code livro_id} (relacao 1-para-1).
 */
@Entity
@Table(name = "livro_capa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LivroCapa {

    @Id
    @Column(name = "livro_id")
    private Long livroId;

    /** Bytes da imagem (mapeado para BYTEA no PostgreSQL). */
    @Column(name = "imagem", nullable = false)
    private byte[] imagem;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;
}
