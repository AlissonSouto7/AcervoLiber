package com.liber.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(

    boolean seedEnabled,

    @NotBlank @Email
    String email,

    @NotBlank
    String nome,

    /** Se vazio, o seeder gera uma senha aleatoria e loga uma unica vez no startup. */
    String password
) {}
