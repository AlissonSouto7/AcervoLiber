package com.liber.controller;

import com.liber.dto.AuditLogResponse;
import com.liber.entity.EventoAuditoria;
import com.liber.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Auditoria (admin)", description = "Trilha de eventos de seguranca — apenas ADMIN")
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Lista eventos de auditoria, opcionalmente filtrados por tipo de evento")
    public Page<AuditLogResponse> listar(
            @RequestParam(required = false) EventoAuditoria evento,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return auditService.consultar(evento, pageable).map(AuditLogResponse::from);
    }
}
