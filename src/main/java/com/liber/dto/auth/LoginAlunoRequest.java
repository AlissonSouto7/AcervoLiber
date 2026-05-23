package com.liber.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginAlunoRequest(

    @NotBlank
    String matricula,

    @NotBlank
    String senha
) {}
