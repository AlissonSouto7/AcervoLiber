package com.liber.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull
    @Min(value = 1, message = "Quantidade de exemplares deve ser ao menos 1")
    Integer quantidadeExemplares
) {}
