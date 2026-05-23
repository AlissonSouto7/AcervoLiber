package com.liber.service;

import com.liber.config.AccountLockoutProperties;
import com.liber.exception.ContaBloqueadaException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementacao in-memory do controle de tentativas de login.
 *
 * <p>Suficiente para uma unica instancia. Em cenario multi-instancia, substituir por
 * uma implementacao Redis (com TTL nativo, que tambem resolve a limpeza de entradas).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InMemoryLoginAttemptService implements LoginAttemptService {

    private final AccountLockoutProperties props;
    private final Clock clock;
    private final ConcurrentMap<String, Registro> registros = new ConcurrentHashMap<>();

    /** Estado por conta: numero de falhas e, se bloqueada, ate quando. */
    private record Registro(int falhas, Instant bloqueadoAte) {}

    @Override
    public void verificarBloqueio(String email, String ip) {
        if (!props.enabled()) {
            return;
        }
        String chave = chave(email, ip);
        Registro r = registros.get(chave);
        if (r == null || r.bloqueadoAte() == null) {
            return;
        }
        if (Instant.now(clock).isBefore(r.bloqueadoAte())) {
            throw new ContaBloqueadaException();
        }
        // Bloqueio expirou — limpa para iniciar uma janela nova
        registros.remove(chave);
    }

    @Override
    public void registrarFalha(String email, String ip) {
        if (!props.enabled()) {
            return;
        }
        registros.compute(chave(email, ip), (chave, atual) -> {
            int falhas = (atual == null || bloqueioExpirado(atual)) ? 1 : atual.falhas() + 1;
            Instant bloqueadoAte = falhas >= props.maxTentativas()
                ? Instant.now(clock).plus(Duration.ofMinutes(props.bloqueioMinutos()))
                : null;
            if (bloqueadoAte != null) {
                log.warn("Combinacao (email,ip)='{}' bloqueada por {} min apos {} tentativas falhas",
                    chave, props.bloqueioMinutos(), falhas);
            }
            return new Registro(falhas, bloqueadoAte);
        });
    }

    @Override
    public void registrarSucesso(String email, String ip) {
        registros.remove(chave(email, ip));
    }

    private boolean bloqueioExpirado(Registro r) {
        return r.bloqueadoAte() != null && !Instant.now(clock).isBefore(r.bloqueadoAte());
    }

    /** Chave composta (e-mail + IP) — defende contra lockout poisoning. */
    private static String chave(String email, String ip) {
        String e = email == null ? "" : email.trim().toLowerCase();
        String i = ip == null ? "" : ip.trim();
        return e + "|" + i;
    }
}
