package com.liber.dto;

import com.liber.entity.Emprestimo;
import com.liber.util.Cpf;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Linha de alerta do dashboard (proximo a vencer / atrasado).
 *
 * <p>Versao reduzida do {@link EmprestimoResponse} com PII minimizada: o nome
 * do aluno fica completo (o bibliotecario precisa identificar quem chamar),
 * mas o CPF vai mascarado (LGPD §14, dados de menores).
 */
public record DashboardAlertaDTO(
    Long emprestimoId,
    String livroTitulo,
    /** Codigo de tombamento do exemplar emprestado — bibliotecario sabe qual copia cobrar. */
    String exemplarCodigo,
    String alunoNome,
    String alunoCpfMascarado,
    String alunoTurma,
    LocalDate dataEmprestimo,
    LocalDate dataDevolucaoPrevista,
    long diasAtraso,
    StatusUrgencia statusUrgencia
) {

    public static DashboardAlertaDTO from(Emprestimo e, LocalDate hoje) {
        long atraso = e.getDataDevolucaoPrevista() != null
            ? Math.max(0, ChronoUnit.DAYS.between(e.getDataDevolucaoPrevista(), hoje))
            : 0;
        return new DashboardAlertaDTO(
            e.getId(),
            e.getExemplar().getLivro().getTitulo(),
            e.getExemplar().getCodigo(),
            e.getAluno().getNome(),
            Cpf.mask(e.getAluno().getCpf()),
            e.getAluno().getTurma(),
            e.getDataEmprestimo(),
            e.getDataDevolucaoPrevista(),
            atraso,
            StatusUrgencia.from(e, hoje)
        );
    }
}
