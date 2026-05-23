package com.liber.dto;

public record LivroRankingDTO(
    Long livroId,
    String titulo,
    String autor,
    Long totalEmprestimos
) {}
