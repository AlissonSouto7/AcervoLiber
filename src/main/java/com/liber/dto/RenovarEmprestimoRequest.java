package com.liber.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Payload da renovacao. Prazo recomeca contando a partir de hoje (nao da data
 * de devolucao prevista anterior) — o limite efetivo continua sendo
 * {@code prazoMaximoDias} validado pelo service.
 */
public record RenovarEmprestimoRequest(

    @NotNull
    @Min(value = 1, message = "Prazo deve ser ao menos 1 dia")
    @Max(value = 3650, message = "Prazo nao pode exceder 3650 dias")
    Integer prazoDias
) {}
