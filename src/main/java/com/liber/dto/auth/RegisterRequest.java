package com.liber.dto.auth;

import com.liber.validation.SenhaForte;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

    @NotBlank
    @Email
    @Size(max = 150)
    String email,

    @NotBlank
    @Size(max = 150)
    String nome,

    @NotBlank
    @SenhaForte
    String senha
) {}
