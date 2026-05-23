package com.liber.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String entidade, Object id) {
        return new ResourceNotFoundException("%s nao encontrado(a) com id=%s".formatted(entidade, id));
    }
}
