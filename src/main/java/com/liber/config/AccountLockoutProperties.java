package com.liber.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bloqueio temporario de conta apos excesso de tentativas de login falhas.
 * Complementa o rate limit por IP — protege contra brute-force distribuido
 * (varios IPs atacando a mesma conta).
 */
@Validated
@ConfigurationProperties(prefix = "app.account-lockout")
public record AccountLockoutProperties(

    boolean enabled,

    @Min(1)
    int maxTentativas,

    @Min(1)
    int bloqueioMinutos
) {}
