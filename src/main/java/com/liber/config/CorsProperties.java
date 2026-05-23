package com.liber.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
    List<String> allowedOrigins,
    List<String> allowedMethods,
    List<String> allowedHeaders,
    boolean allowCredentials
) {
    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("http://localhost:3000", "http://localhost:5173");
        }
        if (allowedMethods == null || allowedMethods.isEmpty()) {
            allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        }
        if (allowedHeaders == null || allowedHeaders.isEmpty()) {
            allowedHeaders = List.of("*");
        }
    }
}
