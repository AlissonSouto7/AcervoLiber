package com.liber.dto;

import com.liber.entity.Livro;
import java.time.Instant;

public record LivroResponse(
    Long id,
    String titulo,
    String autor,
    String isbn,
    Integer ano,
    Integer quantidadeExemplares,
    Integer quantidadeDisponivel,
    String capaUrl,
    boolean capaManual,
    Instant createdAt,
    Instant updatedAt
) {

    public static LivroResponse from(Livro livro) {
        return new LivroResponse(
            livro.getId(),
            livro.getTitulo(),
            livro.getAutor(),
            livro.getIsbn(),
            livro.getAno(),
            livro.getQuantidadeExemplares(),
            livro.getQuantidadeDisponivel(),
            livro.getCapaUrl(),
            livro.isCapaManual(),
            livro.getCreatedAt(),
            livro.getUpdatedAt()
        );
    }
}
