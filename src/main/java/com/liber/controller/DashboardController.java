package com.liber.controller;

import com.liber.dto.DashboardResponse;
import com.liber.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Totais, alertas e ranking")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    // Dashboard expoe contagens globais + lista de emprestimos atrasados/proximos
    // com nome+matricula+turma do aluno responsavel (PII de menor). Restrito a
    // bibliotecario/admin para nao vazar a fila de quem esta com livro atrasado
    // para o resto da escola.
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Retorna agregados do dashboard (bibliotecario/admin)")
    public DashboardResponse obter() {
        return dashboardService.obter();
    }
}
