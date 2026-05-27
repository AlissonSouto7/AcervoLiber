package com.liber.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LivroRequest(

    @NotBlank
    @Size(max = 200)
    String titulo,

    @NotBlank
    @Size(max = 150)
    String autor,

    @Size(max = 20)
    @Pattern(regexp = "^[0-9Xx\\-]*$", message = "ISBN deve conter apenas digitos, hifens ou X")
    String isbn,

    @Min(1000)
    @Max(9999)
    Integer ano,

    /**
     * Quantos exemplares fisicos cadastrar de cara. So usado no POST (cadastro
     * novo) — no PUT (atualizacao) este campo e ignorado, gestao de exemplares
     * acontece por endpoints dedicados (/livros/{id}/exemplares). Cada exemplar
     * ganha um codigo padrao tipo LIB-00042; o bibliotecario pode editar pra
     * casar com a etiqueta fisica que a escola ja tem.
     */
    @Min(value = 1, message = "Informe ao menos 1 exemplar")
    @Max(value = 100, message = "Maximo 100 exemplares por cadastro")
    Integer exemplaresIniciais,

    @Size(max = 2000, message = "Sinopse deve ter no maximo 2000 caracteres")
    String sinopse
) {}
