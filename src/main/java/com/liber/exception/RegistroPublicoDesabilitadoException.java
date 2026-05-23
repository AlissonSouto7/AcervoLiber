package com.liber.exception;

public class RegistroPublicoDesabilitadoException extends RuntimeException {

    public RegistroPublicoDesabilitadoException() {
        super("Registro publico esta desabilitado. Contate um administrador para criar uma conta.");
    }
}
