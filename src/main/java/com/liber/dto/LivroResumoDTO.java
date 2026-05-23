package com.liber.dto;

import com.liber.entity.Livro;

public record LivroResumoDTO(
    Long id,
    String titulo,
    String autor
) {

    public static LivroResumoDTO from(Livro livro) {
        return new LivroResumoDTO(livro.getId(), livro.getTitulo(), livro.getAutor());
    }
}
