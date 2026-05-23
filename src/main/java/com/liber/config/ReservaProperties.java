package com.liber.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.reserva")
public record ReservaProperties(

    /** Dias que uma reserva pendente permanece valida antes de expirar. */
    @Min(1)
    int validadeDias
) {}
