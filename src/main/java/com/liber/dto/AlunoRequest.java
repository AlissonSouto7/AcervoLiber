package com.liber.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AlunoRequest(

    /**
     * CPF do aluno. Aceita com ou sem mascara (123.456.789-01 ou 12345678901).
     * Validacao de digito verificador acontece no servico.
     */
    @NotBlank
    @Pattern(regexp = "^[0-9.\\-]+$", message = "CPF deve conter apenas digitos, pontos e hifen")
    @Size(min = 11, max = 14)
    String cpf,

    @NotBlank
    @Size(max = 150)
    String nome,

    @NotBlank
    @Size(max = 30)
    String turma
) {}
