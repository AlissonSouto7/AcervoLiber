package com.liber.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Remove refresh tokens expirados periodicamente para a tabela nao crescer indefinidamente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupJob {

    private final RefreshTokenService refreshTokenService;

    /** Executa todo dia as 03:00. */
    @Scheduled(cron = "0 0 3 * * *")
    public void limparTokensExpirados() {
        int removidos = refreshTokenService.limparExpirados();
        if (removidos > 0) {
            log.info("Limpeza de refresh tokens: {} expirados removidos", removidos);
        }
    }
}
