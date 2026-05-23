package com.liber.dto;

import com.liber.entity.AuditLog;
import com.liber.entity.EventoAuditoria;
import java.time.Instant;

public record AuditLogResponse(
    Long id,
    EventoAuditoria evento,
    /** Email do ALVO do evento. */
    String usuarioEmail,
    /** Email de QUEM executou a acao (nullable — eventos sem auth). */
    String atorEmail,
    String ip,
    String userAgent,
    String detalhe,
    Instant ocorridoEm
) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
            log.getId(),
            log.getEvento(),
            log.getUsuarioEmail(),
            log.getAtorEmail(),
            log.getIp(),
            log.getUserAgent(),
            log.getDetalhe(),
            log.getOcorridoEm()
        );
    }
}
