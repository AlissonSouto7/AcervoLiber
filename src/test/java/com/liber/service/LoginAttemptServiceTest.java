package com.liber.service;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.liber.config.AccountLockoutProperties;
import com.liber.exception.ContaBloqueadaException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    private static final String EMAIL = "user@test.com";
    private static final String IP = "203.0.113.10";
    private static final String IP_ATACANTE = "198.51.100.42";

    /** Clock mutavel para simular passagem de tempo nos testes. */
    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void avancar(Duration d) {
            this.instant = this.instant.plus(d);
        }

        @Override public Instant instant() { return instant; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
    }

    private MutableClock clock;
    private InMemoryLoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-05-10T12:00:00Z"));
        // enabled, maxTentativas=3, bloqueioMinutos=15
        service = new InMemoryLoginAttemptService(new AccountLockoutProperties(true, 3, 15), clock);
    }

    @Test
    void nao_bloqueia_antes_de_atingir_o_limite() {
        service.registrarFalha(EMAIL, IP);
        service.registrarFalha(EMAIL, IP);

        assertThatNoException().isThrownBy(() -> service.verificarBloqueio(EMAIL, IP));
    }

    @Test
    void bloqueia_ao_atingir_o_limite_de_tentativas() {
        service.registrarFalha(EMAIL, IP);
        service.registrarFalha(EMAIL, IP);
        service.registrarFalha(EMAIL, IP);

        assertThatThrownBy(() -> service.verificarBloqueio(EMAIL, IP))
            .isInstanceOf(ContaBloqueadaException.class);
    }

    @Test
    void login_bem_sucedido_zera_o_contador() {
        service.registrarFalha(EMAIL, IP);
        service.registrarFalha(EMAIL, IP);
        service.registrarSucesso(EMAIL, IP);

        // contador zerado — duas novas falhas nao bloqueiam
        service.registrarFalha(EMAIL, IP);
        service.registrarFalha(EMAIL, IP);

        assertThatNoException().isThrownBy(() -> service.verificarBloqueio(EMAIL, IP));
    }

    @Test
    void bloqueio_expira_apos_o_tempo_configurado() {
        service.registrarFalha(EMAIL, IP);
        service.registrarFalha(EMAIL, IP);
        service.registrarFalha(EMAIL, IP);
        assertThatThrownBy(() -> service.verificarBloqueio(EMAIL, IP))
            .isInstanceOf(ContaBloqueadaException.class);

        clock.avancar(Duration.ofMinutes(16)); // passou os 15 min de bloqueio

        assertThatNoException().isThrownBy(() -> service.verificarBloqueio(EMAIL, IP));
    }

    @Test
    void contas_diferentes_tem_contadores_independentes() {
        service.registrarFalha("a@test.com", IP);
        service.registrarFalha("a@test.com", IP);
        service.registrarFalha("a@test.com", IP);

        assertThatThrownBy(() -> service.verificarBloqueio("a@test.com", IP))
            .isInstanceOf(ContaBloqueadaException.class);
        assertThatNoException().isThrownBy(() -> service.verificarBloqueio("b@test.com", IP));
    }

    /**
     * Defesa contra "lockout poisoning": um atacante de um IP NAO consegue bloquear
     * a conta da vitima fazendo 5 tentativas, porque o lockout e por (email + IP).
     * A vitima continua logando do seu proprio IP.
     */
    @Test
    void atacante_de_um_ip_nao_bloqueia_a_vitima_em_outro_ip() {
        // Atacante de IP_ATACANTE faz 3 falhas contra o email da vitima
        service.registrarFalha(EMAIL, IP_ATACANTE);
        service.registrarFalha(EMAIL, IP_ATACANTE);
        service.registrarFalha(EMAIL, IP_ATACANTE);

        // A combinacao (EMAIL, IP_ATACANTE) esta bloqueada
        assertThatThrownBy(() -> service.verificarBloqueio(EMAIL, IP_ATACANTE))
            .isInstanceOf(ContaBloqueadaException.class);
        // Mas a vitima, vindo do SEU IP, NAO esta bloqueada
        assertThatNoException().isThrownBy(() -> service.verificarBloqueio(EMAIL, IP));
    }

    @Test
    void quando_desabilitado_nunca_bloqueia() {
        InMemoryLoginAttemptService desligado =
            new InMemoryLoginAttemptService(new AccountLockoutProperties(false, 3, 15), clock);

        for (int i = 0; i < 10; i++) {
            desligado.registrarFalha(EMAIL, IP);
        }

        assertThatNoException().isThrownBy(() -> desligado.verificarBloqueio(EMAIL, IP));
    }
}
