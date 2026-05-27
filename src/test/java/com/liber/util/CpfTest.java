package com.liber.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CpfTest {

    @Test
    void normalize_remove_pontuacao() {
        assertThat(Cpf.normalize("123.456.789-09")).isEqualTo("12345678909");
        assertThat(Cpf.normalize("12345678909")).isEqualTo("12345678909");
        assertThat(Cpf.normalize("")).isNull();
        assertThat(Cpf.normalize(null)).isNull();
    }

    @Test
    void isValid_aceita_cpfs_validos_conhecidos() {
        // CPFs validos gerados por algoritmo (digito verificador correto)
        assertThat(Cpf.isValid("123.456.789-09")).isTrue();
        assertThat(Cpf.isValid("12345678909")).isTrue();
        assertThat(Cpf.isValid("529.982.247-25")).isTrue();
    }

    @Test
    void isValid_rejeita_sequencias_triviais() {
        // Estes passam no digito verificador mas sao invalidos por convencao
        assertThat(Cpf.isValid("00000000000")).isFalse();
        assertThat(Cpf.isValid("11111111111")).isFalse();
        assertThat(Cpf.isValid("22222222222")).isFalse();
        assertThat(Cpf.isValid("99999999999")).isFalse();
    }

    @Test
    void isValid_rejeita_digito_verificador_errado() {
        assertThat(Cpf.isValid("123.456.789-00")).isFalse();
        assertThat(Cpf.isValid("12345678900")).isFalse();
    }

    @Test
    void isValid_rejeita_tamanho_errado() {
        assertThat(Cpf.isValid("1234567890")).isFalse();   // 10 digitos
        assertThat(Cpf.isValid("123456789012")).isFalse(); // 12 digitos
        assertThat(Cpf.isValid("")).isFalse();
        assertThat(Cpf.isValid(null)).isFalse();
    }

    @Test
    void format_aplica_pontuacao_padrao() {
        assertThat(Cpf.format("12345678909")).isEqualTo("123.456.789-09");
        assertThat(Cpf.format("123.456.789-09")).isEqualTo("123.456.789-09");
    }

    @Test
    void mask_oculta_meio() {
        assertThat(Cpf.mask("12345678909")).isEqualTo("123.***.***-09");
        assertThat(Cpf.mask("123.456.789-09")).isEqualTo("123.***.***-09");
    }
}
