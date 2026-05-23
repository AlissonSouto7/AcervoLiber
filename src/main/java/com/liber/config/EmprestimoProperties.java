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
    int limitePorAluno
) {}
