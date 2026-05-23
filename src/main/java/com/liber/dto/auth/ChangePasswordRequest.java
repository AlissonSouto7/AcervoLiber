package com.liber.dto.auth;

import com.liber.validation.SenhaForte;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(

    @NotBlank
    String senhaAtual,

    @NotBlank
    @SenhaForte
    String senhaNova
) {}
