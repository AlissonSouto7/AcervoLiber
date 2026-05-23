package com.liber.entity;

/** Estados de uma reserva de livro. */
public enum StatusReserva {
    /** Aguardando o aluno retirar o livro / o bibliotecario confirmar. */
    PENDENTE,
    /** Bibliotecario confirmou — virou um emprestimo. */
    CONFIRMADA,
    /** Bibliotecario recusou. */
    RECUSADA,
    /** Aluno cancelou. */
    CANCELADA,
    /** Nao foi retirada dentro do prazo de validade. */
    EXPIRADA
}
