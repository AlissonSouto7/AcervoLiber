package com.liber.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Valida senha conforme politica do projeto:
 *
 * <ul>
 *   <li>10 a 100 caracteres</li>
 *   <li>pelo menos 1 letra maiuscula</li>
 *   <li>pelo menos 1 letra minuscula</li>
 *   <li>pelo menos 1 digito</li>
 *   <li>pelo menos 1 caractere especial</li>
 *   <li>nao pode ser uma senha trivial conhecida (lista negra)</li>
 * </ul>
 *
 * <p>A regra "nao pode ser igual ao nome/email/cpf do usuario" e aplicada no
 * service (precisa do contexto que o validator nao tem).
 */
public class SenhaForteValidator implements ConstraintValidator<SenhaForte, String> {

    private static final int MIN = 10;
    private static final int MAX = 100;

    private static final Pattern TEM_MAIUSCULA = Pattern.compile("[A-ZÁÉÍÓÚÂÊÎÔÛÃÕÀÇ]");
    private static final Pattern TEM_MINUSCULA = Pattern.compile("[a-záéíóúâêîôûãõàç]");
    private static final Pattern TEM_DIGITO = Pattern.compile("\\d");
    private static final Pattern TEM_ESPECIAL = Pattern.compile("[^A-Za-zÁÉÍÓÚÂÊÎÔÛÃÕÀÇáéíóúâêîôûãõàç0-9]");

    /** Senhas comuns rejeitadas mesmo passando nas regras de composicao. */
    private static final Set<String> SENHAS_COMUNS = Set.of(
        "password1", "password123", "senha12345",
        "qwertyuiop", "qwerty12345", "admin12345", "1q2w3e4r5t",
        "abcdefghij", "liber12345"
        // Removido "12345678910" e similares — a regra de 1 letra + 1 especial
        // ja bloqueia automaticamente.
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // nulidade e responsabilidade do @NotBlank
        }

        // Tamanho
        if (value.length() < MIN || value.length() > MAX) {
            return mensagem(context, "Senha deve ter entre " + MIN + " e " + MAX + " caracteres.");
        }

        // Composicao
        if (!TEM_MAIUSCULA.matcher(value).find()) {
            return mensagem(context, "Senha deve conter ao menos uma letra MAIUSCULA.");
        }
        if (!TEM_MINUSCULA.matcher(value).find()) {
            return mensagem(context, "Senha deve conter ao menos uma letra minuscula.");
        }
        if (!TEM_DIGITO.matcher(value).find()) {
            return mensagem(context, "Senha deve conter ao menos um numero.");
        }
        if (!TEM_ESPECIAL.matcher(value).find()) {
            return mensagem(context, "Senha deve conter ao menos um caractere especial (ex.: ! @ # $ % & *).");
        }

        // Lista negra
        if (SENHAS_COMUNS.contains(value.toLowerCase())) {
            return mensagem(context, "Senha muito comum. Escolha algo mais unico.");
        }

        return true;
    }

    /** Substitui a mensagem default pela especifica do erro encontrado. */
    private static boolean mensagem(ConstraintValidatorContext ctx, String msg) {
        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
        return false;
    }
}
