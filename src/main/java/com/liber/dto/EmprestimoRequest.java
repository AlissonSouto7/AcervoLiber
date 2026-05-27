package com.liber.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EmprestimoRequest(

    /**
     * ID do exemplar fisico que esta sendo emprestado (codigo de tombamento da
     * escola). O bibliotecario escolhe a copia exata na tela — antes era so
     * "qualquer copia do livro X", agora cada copia tem identidade propria.
     */
    @NotNull
    Long exemplarId,

    @NotNull
    Long alunoId,

    @NotNull
    @Min(value = 1, message = "Prazo deve ser ao menos 1 dia")
    // Defesa contra overflow: sem @Max, Integer.MAX_VALUE em prazoDias estoura
    // LocalDate.plusDays(...) com DateTimeException -> 500. 3650 = 10 anos,
    // muito acima do max real (30 dias via EmprestimoProperties).
    @Max(value = 3650, message = "Prazo nao pode exceder 3650 dias")
    Integer prazoDias
) {}
