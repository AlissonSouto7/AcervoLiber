package com.liber.entity;

/** Tipos de evento registrados na trilha de auditoria de seguranca. */
public enum EventoAuditoria {
    LOGIN_SUCESSO,
    LOGIN_FALHA,
    LOGIN_BLOQUEADO,
    LOGOUT,
    TROCA_SENHA,
    PERFIL_ATUALIZADO,
    USUARIO_CRIADO,
    USUARIO_ATIVADO,
    USUARIO_DESATIVADO,
    /** Refresh token revogado foi reapresentado — indicio de roubo de token. */
    REFRESH_REUSO,
    /** Requisicao autenticada mas sem permissao (403) — probing/insider. */
    ACESSO_NEGADO,
    /** Empréstimo de livro registrado (saida de bem material). */
    EMPRESTIMO_REGISTRADO,
    /** Prazo de devolucao estendido (renovacao pelo bibliotecario). */
    EMPRESTIMO_RENOVADO,
    /** Empréstimo devolvido. */
    EMPRESTIMO_DEVOLVIDO,
    /** Estoque inconsistente — incremento de devolucao nao surtiu efeito (clamp). */
    ESTOQUE_DIVERGENCIA,
    /** Aluno criou uma reserva — segura um exemplar. */
    RESERVA_CRIADA,
    /** Bibliotecario confirmou uma reserva — gerou empréstimo. */
    RESERVA_CONFIRMADA,
    /** Bibliotecario recusou uma reserva — exemplar liberado. */
    RESERVA_RECUSADA,
    /** Aluno cancelou a propria reserva — exemplar liberado. */
    RESERVA_CANCELADA,
    /** Reserva nao retirada dentro do prazo — exemplar liberado pelo job. */
    RESERVA_EXPIRADA
}
