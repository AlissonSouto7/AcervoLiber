package com.liber.exception;

public class ContaBloqueadaException extends RuntimeException {

    public ContaBloqueadaException() {
        super("Conta temporariamente bloqueada por excesso de tentativas de login. "
            + "Aguarde alguns minutos e tente novamente.");
    }
}
