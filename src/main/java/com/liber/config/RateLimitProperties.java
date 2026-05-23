package com.liber.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.rate-limit.login")
public record RateLimitProperties(

    boolean enabled,

    @Min(1)
    int capacity,

    @Min(1)
    int refillTokens,

    @Min(1)
    int refillPeriodSeconds
) {}
