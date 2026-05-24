package com.liber.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.emprestimo")
public record EmprestimoProperties(

    @Min(1)
    int prazoPadraoDias,

    @Min(1)
    int prazoMaximoDias,

    @Min(1)
    int limitePorAluno,

    /**
     * Numero maximo de renovacoes permitidas por emprestimo. Default 2 — padrao
     * de biblioteca escolar. 0 desabilita renovacao.
     */
    @Min(0)
    int maxRenovacoes
) {}
