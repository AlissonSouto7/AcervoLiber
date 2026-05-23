package com.liber.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(

    @NotBlank
    @Size(min = 32, message = "JWT secret precisa ter no minimo 32 bytes para HS256")
    String secret,

    /** TTL do access token (JWT). */
    @Min(60_000)
    long expirationMs,

    /** TTL do refresh token (opaco, em banco). */
    @Min(60_000)
    long refreshExpirationMs,

    @NotBlank
    String issuer
) {}
