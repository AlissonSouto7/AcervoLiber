package com.liber.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

public class SenhaForteValidator implements ConstraintValidator<SenhaForte, String> {

    private static final int MIN = 10;
    private static final int MAX = 100;

    /** Senhas triviais rejeitadas independente do comprimento. */
    private static final Set<String> SENHAS_COMUNS = Set.of(
        "1234567890", "12345678910", "123456789", "12345678",
        "password", "password1", "password123", "senha12345",
        "qwertyuiop", "qwerty12345", "admin12345", "1q2w3e4r5t",
        "0000000000", "1111111111", "liber", "abcdefghij"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // nulidade e responsabilidade do @NotBlank
        }
        if (value.length() < MIN || value.length() > MAX) {
            return false;
        }
        return !SENHAS_COMUNS.contains(value.toLowerCase());
    }
}
