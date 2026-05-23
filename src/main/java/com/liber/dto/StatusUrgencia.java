package com.liber.dto;

import com.liber.entity.Emprestimo;
import com.liber.entity.SituacaoEmprestimo;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public enum StatusUrgencia {
    DEVOLVIDO,
    VERDE,
    AMARELO,
    VERMELHO;

    private static final long DIAS_AMARELO = 2;

    public static StatusUrgencia from(Emprestimo emprestimo, LocalDate hoje) {
        if (emprestimo.getSituacao() == SituacaoEmprestimo.DEVOLVIDO) {
            return DEVOLVIDO;
        }
        // Defesa: dataDevolucaoPrevista deveria ser @NotNull, mas drift de schema
        // ou JPA load de linha corrompida quebraria a tabela inteira no .map(...).
        // Fallback seguro: VERDE.
        if (emprestimo.getDataDevolucaoPrevista() == null) {
            return VERDE;
        }
        long diasRestantes = ChronoUnit.DAYS.between(hoje, emprestimo.getDataDevolucaoPrevista());
        if (diasRestantes < 0) {
            return VERMELHO;
        }
        if (diasRestantes <= DIAS_AMARELO) {
            return AMARELO;
        }
        return VERDE;
    }
}
