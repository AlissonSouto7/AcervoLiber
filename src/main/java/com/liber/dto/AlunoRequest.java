package com.liber.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlunoRequest(

    @NotBlank
    @Size(max = 30)
    String matricula,

    @NotBlank
    @Size(max = 150)
    String nome,

    @NotBlank
    @Size(max = 30)
    String turma
) {}
