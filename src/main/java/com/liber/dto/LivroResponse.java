package com.liber.dto;

import com.liber.entity.Livro;
import java.time.Instant;

public record LivroResponse(
    Long id,
    String titulo,
    String autor,
    String isbn,
    Integer ano,
    /** Total de exemplares fisicos cadastrados (qualquer situacao exceto EXTRAVIADO). */
    int exemplaresTotal,
    /** Quantos desses estao DISPONIVEL pra emprestimo/reserva imediato. */
    int exemplaresDisponiveis,
    String capaUrl,
    boolean capaManual,
    String sinopse,
    Instant createdAt,
    Instant updatedAt
) {

    public static LivroResponse from(Livro livro, int exemplaresTotal, int exemplaresDisponiveis) {
        return new LivroResponse(
            livro.getId(),
            livro.getTitulo(),
            livro.getAutor(),
            livro.getIsbn(),
            livro.getAno(),
            exemplaresTotal,
            exemplaresDisponiveis,
            livro.getCapaUrl(),
            livro.isCapaManual(),
            livro.getSinopse(),
            livro.getCreatedAt(),
            livro.getUpdatedAt()
        );
    }
}
