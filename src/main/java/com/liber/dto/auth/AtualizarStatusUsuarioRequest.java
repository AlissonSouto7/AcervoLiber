package com.liber.dto.auth;

import jakarta.validation.constraints.NotNull;

public record AtualizarStatusUsuarioRequest(

    @NotNull
    Boolean ativo
) {}
