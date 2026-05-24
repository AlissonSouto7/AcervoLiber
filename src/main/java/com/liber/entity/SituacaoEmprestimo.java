package com.liber.entity;

public enum SituacaoEmprestimo {
    ATIVO,
    DEVOLVIDO,
    /**
     * Lancamento incorreto cancelado pelo bibliotecario via DELETE /emprestimos/{id}.
     * O livro foi devolvido ao estoque automaticamente; mantido na tabela para
     * preservar trilha de auditoria (FK de reservas confirmadas continua valida).
     */
    CANCELADO
}
