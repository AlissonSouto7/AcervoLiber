package com.liber.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "AcervoLiber API",
        version = "v1",
        description = "API do sistema de biblioteca escolar",
        contact = @Contact(name = "AcervoLiber")
    ),
    servers = {@Server(url = "/", description = "default")},
    security = {@SecurityRequirement(name = "bearerAuth")}
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT obtido em POST /api/v1/auth/login"
)
public class OpenApiConfig {
}
