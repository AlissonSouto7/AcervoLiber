package com.liber.dto;

import java.util.List;

public record DashboardResponse(
    Totais totais,
    List<DashboardAlertaDTO> alertasProximaDevolucao,
    List<DashboardAlertaDTO> alertasAtrasados,
    List<LivroRankingDTO> livrosMaisEmprestados
) {

    public record Totais(
        long totalLivros,
        long totalAlunos,
        long emprestimosAtivos,
        long emprestimosAtrasados
    ) {}
}
