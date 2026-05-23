package com.liber.dto.auth;

import com.liber.validation.SenhaForte;
import jakarta.validation.constraints.NotBlank;

/** Senha provisoria definida pelo bibliotecario ao criar o acesso de um aluno. */
public record CriarAcessoAlunoRequest(

    @NotBlank
    @SenhaForte
    String senhaInicial
) {}
