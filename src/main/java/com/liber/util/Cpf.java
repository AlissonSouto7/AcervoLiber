package com.liber.util;

/**
 * Helpers de CPF — normalizacao (so digitos) e validacao do digito verificador.
 *
 * <p>Usado no cadastro/auto-cadastro de aluno e no login do aluno. A escola usa
 * CPF como identificador unico porque nao tem matricula formal pra todos os
 * alunos — CPF e o que o aluno sabe de cor / o que os pais informam.
 */
public final class Cpf {

    private Cpf() {}

    /** Remove qualquer caractere que nao seja digito. Retorna null se entrada for null. */
    public static String normalize(String cpf) {
        if (cpf == null) return null;
        String onlyDigits = cpf.replaceAll("\\D", "");
        return onlyDigits.isEmpty() ? null : onlyDigits;
    }

    /**
     * Valida um CPF (11 digitos + digito verificador correto). Aceita com ou
     * sem formatacao — normaliza antes de validar.
     *
     * <p>Rejeita CPFs invalidos conhecidos (todos digitos iguais: 00000000000,
     * 11111111111, ..., 99999999999) que passam no digito verificador mas
     * nao sao validos legalmente.
     */
    public static boolean isValid(String cpf) {
        String c = normalize(cpf);
        if (c == null || c.length() != 11) return false;

        // Rejeita sequencias triviais (00000000000, 11111111111, etc.)
        boolean todosIguais = true;
        for (int i = 1; i < 11; i++) {
            if (c.charAt(i) != c.charAt(0)) {
                todosIguais = false;
                break;
            }
        }
        if (todosIguais) return false;

        // Calcula primeiro digito verificador
        int soma = 0;
        for (int i = 0; i < 9; i++) {
            soma += (c.charAt(i) - '0') * (10 - i);
        }
        int d1 = 11 - (soma % 11);
        if (d1 >= 10) d1 = 0;
        if (d1 != c.charAt(9) - '0') return false;

        // Calcula segundo digito verificador
        soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += (c.charAt(i) - '0') * (11 - i);
        }
        int d2 = 11 - (soma % 11);
        if (d2 >= 10) d2 = 0;
        return d2 == c.charAt(10) - '0';
    }

    /** Formata CPF com pontuacao: 12345678901 -> 123.456.789-01. */
    public static String format(String cpf) {
        String c = normalize(cpf);
        if (c == null || c.length() != 11) return cpf;
        return c.substring(0, 3) + "." + c.substring(3, 6) + "." + c.substring(6, 9) + "-" + c.substring(9);
    }

    /**
     * Mascara CPF pra exibicao LGPD: 123.***.***-01 (mostra primeiros 3 e
     * ultimos 2). Usado em telas vistas por terceiros (reservas pendentes
     * passa o crachá no balcao, outros alunos veem de lado, etc).
     */
    public static String mask(String cpf) {
        String c = normalize(cpf);
        if (c == null || c.length() != 11) return cpf;
        return c.substring(0, 3) + ".***.***-" + c.substring(9);
    }
}
