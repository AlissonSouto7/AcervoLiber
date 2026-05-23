package com.liber.dto;

/**
 * Resumo da situacao de reservas/emprestimos de um aluno — usado pelo catalogo
 * para mostrar quantas "vagas" ainda restam antes de atingir o limite.
 *
 * @param emprestimosAtivos emprestimos em aberto do aluno
 * @param reservasPendentes reservas ainda nao resolvidas
 * @param limite            teto combinado de emprestimos + reservas por aluno
 */
public record ReservaResumoResponse(
    int emprestimosAtivos,
    int reservasPendentes,
    int limite
) {
    // "vagas disponiveis" = limite - ativos - pendentes — calculado no frontend
    // a partir destes 3 campos (o Jackson so serializa componentes do record).
}
