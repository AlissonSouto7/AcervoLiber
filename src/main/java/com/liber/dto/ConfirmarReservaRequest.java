package com.liber.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfirmarReservaRequest(

    @NotNull
    @Min(value = 1, message = "Prazo deve ser ao menos 1 dia")
    @Max(value = 3650, message = "Prazo nao pode exceder 3650 dias")
    Integer prazoDias,

    /**
     * Opcional. Se enviado, troca o exemplar reservado pelo escolhido aqui —
     * util quando o bibliotecario tem outra copia em maos (LIB-00002 em vez
     * do LIB-00001 que foi reservado). Se null, usa o exemplar ja reservado.
     */
    Long exemplarId
) {}
