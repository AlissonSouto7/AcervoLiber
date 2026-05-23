package com.liber.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AtualizarPerfilRequest(

    @NotBlank
    @Size(max = 150)
    // Bloqueia HTML/script injection no nome (visto no pentest da Fase 7: o
    // payload "<script>alert(1)</script>" passava sem validacao e ficava
    // persistido). Aceita letras Unicode (acentos/cedilhas), digitos, espacos,
    // hifens, apostrofos, virgulas e ponto — cobre nomes normais.
    @Pattern(
        regexp = "^[\\p{L}\\p{N} .,'\\-]+$",
        message = "Nome contem caracteres invalidos."
    )
    String nome
) {}
