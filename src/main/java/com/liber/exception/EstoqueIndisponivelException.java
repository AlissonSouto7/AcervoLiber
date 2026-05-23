package com.liber.exception;

public class EstoqueIndisponivelException extends BusinessException {

    public EstoqueIndisponivelException(Long livroId) {
        super("Nao ha exemplares disponiveis para o livro id=%s".formatted(livroId));
    }
}
