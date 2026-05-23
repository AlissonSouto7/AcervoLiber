package com.liber.dto.auth;

import com.liber.entity.Role;
import com.liber.validation.SenhaForte;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CriarUsuarioRequest(

    @NotBlank
    @Email
    @Size(max = 150)
    String email,

    @NotBlank
    @Size(max = 150)
    // Mesma regex de AtualizarPerfilRequest — bloqueia HTML/script no nome.
    @Pattern(
        regexp = "^[\\p{L}\\p{N} .,'\\-]+$",
        message = "Nome contem caracteres invalidos."
    )
    String nome,

    @NotBlank
    @SenhaForte
    String senha,

    @NotNull
    Role role
) {}
