package com.liber.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.liber.entity.Emprestimo;
import com.liber.entity.SituacaoEmprestimo;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class StatusUrgenciaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 5, 10);

    @Test
    void devolvido_retorna_DEVOLVIDO_independente_da_data() {
        Emprestimo e = emp(SituacaoEmprestimo.DEVOLVIDO, HOJE.minusDays(10));
        assertThat(StatusUrgencia.from(e, HOJE)).isEqualTo(StatusUrgencia.DEVOLVIDO);
    }

    @Test
    void ativo_com_prazo_passado_retorna_VERMELHO() {
        Emprestimo e = emp(SituacaoEmprestimo.ATIVO, HOJE.minusDays(1));
        assertThat(StatusUrgencia.from(e, HOJE)).isEqualTo(StatusUrgencia.VERMELHO);
    }

    @Test
    void ativo_com_prazo_hoje_ou_em_ate_2_dias_retorna_AMARELO() {
        assertThat(StatusUrgencia.from(emp(SituacaoEmprestimo.ATIVO, HOJE), HOJE))
            .isEqualTo(StatusUrgencia.AMARELO);
        assertThat(StatusUrgencia.from(emp(SituacaoEmprestimo.ATIVO, HOJE.plusDays(1)), HOJE))
            .isEqualTo(StatusUrgencia.AMARELO);
        assertThat(StatusUrgencia.from(emp(SituacaoEmprestimo.ATIVO, HOJE.plusDays(2)), HOJE))
            .isEqualTo(StatusUrgencia.AMARELO);
    }

    @Test
    void ativo_com_prazo_maior_que_2_dias_retorna_VERDE() {
        assertThat(StatusUrgencia.from(emp(SituacaoEmprestimo.ATIVO, HOJE.plusDays(3)), HOJE))
            .isEqualTo(StatusUrgencia.VERDE);
        assertThat(StatusUrgencia.from(emp(SituacaoEmprestimo.ATIVO, HOJE.plusDays(30)), HOJE))
            .isEqualTo(StatusUrgencia.VERDE);
    }

    private static Emprestimo emp(SituacaoEmprestimo situacao, LocalDate dataDevolucaoPrevista) {
        return Emprestimo.builder()
            .situacao(situacao)
            .dataDevolucaoPrevista(dataDevolucaoPrevista)
            .build();
    }
}
