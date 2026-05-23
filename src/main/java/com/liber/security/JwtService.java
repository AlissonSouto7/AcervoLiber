package com.liber.security;

import com.liber.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JwtService {

    /**
     * Valor literal do default de desenvolvimento em {@code application.properties}.
     * Se o secret em runtime for igual a esta string E o perfil ativo for {@code prod},
     * o startup aborta — evita rodar producao com chave publica que qualquer um forja.
     */
    private static final String SECRET_DEFAULT_DEV =
        "dev-only-secret-trocar-em-prod-com-no-minimo-32-bytes-ABCDEF1234567890";

    private final JwtProperties props;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtService(JwtProperties props, Clock clock, Environment environment) {
        this.props = props;
        this.clock = clock;
        validarSecret(props.secret(), environment);
        this.signingKey = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Recusa o secret default em producao; em dev apenas avisa. Se {@code JWT_SECRET}
     * nao for definido no deploy de prod, o {@code application.properties} cai na
     * string literal de desenvolvimento e qualquer pessoa que conheca o codigo
     * forja tokens ADMIN — fail-fast bloqueia esse cenario antes do startup terminar.
     */
    private static void validarSecret(String secret, Environment environment) {
        if (!SECRET_DEFAULT_DEV.equals(secret)) {
            return;
        }
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile)) {
                throw new IllegalStateException(
                    "JWT_SECRET nao foi definido em producao — o default de desenvolvimento "
                        + "(string literal em application.properties) e publico. "
                        + "Gere um secret forte (openssl rand -base64 48) e exporte JWT_SECRET.");
            }
        }
        log.warn("JWT_SECRET esta usando o default de desenvolvimento — NUNCA use em producao.");
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, Map.of());
    }

    public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        Instant now = Instant.now(clock);
        Instant expiration = now.plusMillis(props.expirationMs());

        return Jwts.builder()
            .claims(extraClaims)
            .subject(userDetails.getUsername())
            .issuer(props.issuer())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public Instant extractIssuedAt(String token) {
        return parse(token).getIssuedAt().toInstant();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = parse(token);
            return claims.getSubject().equals(userDetails.getUsername())
                && claims.getExpiration().toInstant().isAfter(Instant.now(clock));
        } catch (JwtException e) {
            log.debug("Token invalido: {}", e.getMessage());
            return false;
        }
    }

    public long getExpirationMs() {
        return props.expirationMs();
    }

    private Claims parse(String token) {
        return Jwts.parser()
            .clock(() -> Date.from(Instant.now(clock)))
            .verifyWith(signingKey)
            .requireIssuer(props.issuer())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
