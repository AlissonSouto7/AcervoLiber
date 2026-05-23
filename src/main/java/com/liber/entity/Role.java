package com.liber.entity;

public enum Role {
    ADMIN,
    BIBLIOTECARIO,
    ALUNO;

    public String authority() {
        return "ROLE_" + name();
    }
}
