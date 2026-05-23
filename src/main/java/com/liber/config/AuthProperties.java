package com.liber.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(

    /** Quando false, POST /auth/register retorna 403. Admins criam usuarios via POST /usuarios. */
    boolean publicRegisterEnabled
) {}
