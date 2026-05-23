package com.liber.dto.auth;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresInMs,
    UsuarioResponse usuario
) {
    public static LoginResponse of(String accessToken, String refreshToken,
                                   long expiresInMs, UsuarioResponse usuario) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresInMs, usuario);
    }
}
