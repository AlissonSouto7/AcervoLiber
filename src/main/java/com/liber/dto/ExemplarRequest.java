package com.liber.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body pra criar/editar um exemplar avulso. Codigo e opcional: se nao informado,
 * o sistema gera o proximo da sequence ({@code LIB-XXXXX}). O bibliotecario pode
 * digitar pra casar com a etiqueta fisica que a escola ja tem.
 */
public record ExemplarRequest(
    @Size(max = 50, message = "Codigo deve ter no maximo 50 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9\\-_/.]*$",
        message = "Codigo aceita letras, digitos, hifen, underscore, barra e ponto")
    String codigo
) {}
