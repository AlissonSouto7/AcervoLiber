package com.liber.dto;

import jakarta.validation.constraints.NotNull;

public record CriarReservaRequest(

    @NotNull
    Long livroId
) {}
