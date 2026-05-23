package com.liber.service;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Expira reservas pendentes nao retiradas dentro do prazo de validade,
 * liberando os exemplares que estavam segurados.
 *
 * <p>O {@code AtomicBoolean rodando} evita que duas execucoes se sobreponham na
 * mesma instancia (mesmo padrao do {@code CapaBackfillJob}). Para protecao em
 * cluster multi-instancia, seria necessario lock distribuido (ShedLock) — hoje
 * o deployment tem 1 instancia, deferido pro cleanup pass.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReservaExpiracaoJob {

    private final ReservaService reservaService;

    /** Evita que duas execucoes se sobreponham nesta instancia. */
    private final AtomicBoolean rodando = new AtomicBoolean(false);

    /** Executa todo dia as 03:30. */
    @Scheduled(cron = "0 30 3 * * *")
    public void expirarReservas() {
        if (!rodando.compareAndSet(false, true)) {
            log.warn("Job de expiracao de reservas ja em andamento — pulando esta execucao");
            return;
        }
        try {
            int expiradas = reservaService.expirarVencidas();
            if (expiradas > 0) {
                log.info("Reservas expiradas: {}", expiradas);
            }
        } finally {
            rodando.set(false);
        }
    }
}
