package com.liber.service;

import com.liber.config.JwtProperties;
import com.liber.entity.EventoAuditoria;
import com.liber.entity.RefreshToken;
import com.liber.entity.Usuario;
import com.liber.exception.RefreshTokenInvalidoException;
import com.liber.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestao de refresh tokens opacos com rotacao e deteccao de reuso.
 *
 * <p>O token em texto puro so existe na resposta ao cliente — no banco guardamos
 * apenas o hash SHA-256. Cada uso rotaciona o token (revoga o antigo, emite um novo).
 * Se um token ja revogado for reapresentado, assume-se roubo e toda a familia de
 * tokens do usuario e revogada.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository repository;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final AuditService auditService;

    /** Resultado de uma rotacao: usuario dono + novo refresh token em texto puro. */
    public record Rotacao(Usuario usuario, String novoRefreshToken) {}

    @Transactional
    public String gerar(Usuario usuario) {
        String raw = gerarTokenAleatorio();
        RefreshToken token = RefreshToken.builder()
            .tokenHash(hash(raw))
            .usuario(usuario)
            .expiresAt(Instant.now(clock).plusMillis(jwtProperties.refreshExpirationMs()))
            .build();
        repository.save(token);
        return raw;
    }

    @Transactional
    public Rotacao rotacionar(String rawToken) {
        RefreshToken atual = repository.findByTokenHash(hash(rawToken))
            .orElseThrow(() -> new RefreshTokenInvalidoException("Refresh token invalido"));

        Instant agora = Instant.now(clock);

        if (atual.isRevogado()) {
            // Token revogado reapresentado — possivel roubo. Revoga toda a familia.
            int revogados = repository.revogarTodosDoUsuario(atual.getUsuario().getId(), agora);
            log.warn("Reuso de refresh token detectado para usuario id={} — {} tokens revogados",
                atual.getUsuario().getId(), revogados);
            // Auditoria: sinal mais importante da trilha de seguranca — token roubado.
            auditService.registrar(EventoAuditoria.REFRESH_REUSO,
                atual.getUsuario().getEmail(),
                "Reuso detectado — " + revogados + " sessoes encerradas");
            throw new RefreshTokenInvalidoException(
                "Refresh token reutilizado — todas as sessoes foram encerradas por seguranca");
        }
        if (atual.isExpirado(agora)) {
            throw new RefreshTokenInvalidoException("Refresh token expirado");
        }

        atual.setRevokedAt(agora);
        repository.save(atual);

        String novo = gerar(atual.getUsuario());
        return new Rotacao(atual.getUsuario(), novo);
    }

    /**
     * Revoga o refresh token apresentado, mas APENAS se ele pertencer ao usuario
     * autenticado (validacao de ownership). Se nao pertencer, ignora silenciosamente
     * — nao revela se o token existe nem revoga sessao de outra pessoa. Cenario que
     * essa checagem fecha: atacante sem auth (ou de outra conta) tentando revogar
     * o refresh de uma vitima.
     */
    @Transactional
    public void revogarSeDoUsuario(String rawToken, String emailDono) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            if (emailDono == null
                    || !emailDono.equalsIgnoreCase(token.getUsuario().getEmail())) {
                log.warn("Tentativa de revogar refresh token de outro usuario por '{}'", emailDono);
                return;
            }
            if (!token.isRevogado()) {
                token.setRevokedAt(Instant.now(clock));
                repository.save(token);
            }
        });
    }

    /**
     * Revoga TODOS os refresh tokens ativos de um usuario — encerra todas as sessoes
     * dele. Chamado em: deteccao de reuso, troca de senha e desativacao da conta.
     */
    @Transactional
    public int revogarTodosDoUsuario(Long usuarioId) {
        int revogados = repository.revogarTodosDoUsuario(usuarioId, Instant.now(clock));
        if (revogados > 0) {
            log.info("Revogados {} refresh tokens do usuario id={}", revogados, usuarioId);
        }
        return revogados;
    }

    @Transactional
    public int limparExpirados() {
        return repository.deleteByExpiresAtBefore(Instant.now(clock));
    }

    private static String gerarTokenAleatorio() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Hash SHA-256 em hex — visibilidade de pacote para uso em testes. */
    static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", e);
        }
    }
}
