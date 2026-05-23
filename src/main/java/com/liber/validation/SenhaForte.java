package com.liber.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Valida uma senha conforme a politica do sistema: comprimento minimo e
 * rejeicao de senhas triviais. Alinhado ao NIST 800-63B — prioriza comprimento
 * em vez de regras de composicao obrigatorias.
 */
@Documented
@Constraint(validatedBy = SenhaForteValidator.class)
@Target({java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SenhaForte {

    String message() default "Senha deve ter ao menos 10 caracteres e nao pode ser uma senha comum";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
