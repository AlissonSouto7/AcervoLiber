package com.liber.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginAlunoRequest(

    /**
     * CPF do aluno. Aceita com mascara ou so digitos. Em V18 substituiu o
     * campo matricula (a escola nao tem matricula formal pros alunos).
     */
    @NotBlank
    @Pattern(regexp = "^[0-9.\\-]+$", message = "CPF deve conter apenas digitos, pontos e hifen")
    String cpf,

    @NotBlank
    String senha
) {}
