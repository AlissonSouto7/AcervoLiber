package com.liber.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.liber.config.JwtProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

    private static final String SECRET = "test-secret-com-pelo-menos-32-bytes-para-HS256-1234567890";
    private static final long EXPIRATION_MS = 3_600_000L; // 1h
    private static final long REFRESH_MS = 604_800_000L;  // 7 dias
    private static final String ISSUER = "test";

    private static final Instant T0 = Instant.parse("2026-05-10T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

    /** Environment de teste — sem profiles ativos, nao dispara o fail-fast de prod. */
    private static final MockEnvironment ENV = new MockEnvironment();

    private final JwtProperties props = new JwtProperties(SECRET, EXPIRATION_MS, REFRESH_MS, ISSUER);
    private final JwtService service = new JwtService(props, CLOCK, ENV);

    private static UserDetails userDetails(String email) {
        return User.withUsername(email).password("x").authorities("ROLE_BIBLIOTECARIO").build();
    }

    @Test
    void token_gerado_e_valido_e_carrega_o_subject_correto() {
        UserDetails ud = userDetails("user@test.com");

        String token = service.generateToken(ud);

        assertThat(token).isNotBlank();
        assertThat(service.extractUsername(token)).isEqualTo("user@test.com");
        assertThat(service.isTokenValid(token, ud)).isTrue();
    }

    @Test
    void token_expirado_e_invalido() {
        UserDetails ud = userDetails("user@test.com");
        String token = service.generateToken(ud);

        // Avanca o relogio para 2h apos a emissao (tokens valem 1h)
        Clock futuroClock = Clock.fixed(T0.plusSeconds(2 * 3600), ZoneOffset.UTC);
        JwtService servicoNoFuturo = new JwtService(props, futuroClock, ENV);

        assertThat(servicoNoFuturo.isTokenValid(token, ud)).isFalse();
    }

    @Test
    void token_de_outro_issuer_e_invalido() {
        JwtProperties propsOutroIssuer = new JwtProperties(SECRET, EXPIRATION_MS, REFRESH_MS, "outro");
        JwtService servicoOutroIssuer = new JwtService(propsOutroIssuer, CLOCK, ENV);

        String tokenDoOutro = servicoOutroIssuer.generateToken(userDetails("user@test.com"));

        assertThat(service.isTokenValid(tokenDoOutro, userDetails("user@test.com"))).isFalse();
    }

    @Test
    void token_para_outro_usuario_e_invalido() {
        String token = service.generateToken(userDetails("user@test.com"));

        assertThat(service.isTokenValid(token, userDetails("outro@test.com"))).isFalse();
    }
}
