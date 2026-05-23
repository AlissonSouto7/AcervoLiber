package com.liber.dto;

import com.liber.entity.Emprestimo;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Linha de alerta do dashboard (proximo a vencer / atrasado).
 *
 * <p>Versao reduzida do {@link EmprestimoResponse} com PII minimizada: o nome
 * do aluno fica completo (o bibliotecario precisa identificar quem chamar),
 * mas a matricula — identificador unico que segue o aluno por toda a vida
 * escolar — vai mascarada. Reduz exposicao em cenarios em que a tela fica
 * visivel a terceiros (visitante atras do balcao, foto de tela, cache de
 * proxy). Cobre achado da Fase 6.A.alta.2.
 */
public record DashboardAlertaDTO(
    Long emprestimoId,
    String livroTitulo,
    String alunoNome,
    String alunoMatriculaMascarada,
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
            e.getLivro().getTitulo(),
            e.getAluno().getNome(),
            mascarar(e.getAluno().getMatricula()),
            e.getAluno().getTurma(),
            e.getDataEmprestimo(),
            e.getDataDevolucaoPrevista(),
            atraso,
            StatusUrgencia.from(e, hoje)
        );
    }

    /**
     * Mantem os 5 primeiros caracteres da matricula visiveis e troca o resto
     * por "*". Ex.: "202700100" -> "20270****", "12345" -> "12345" (curtas
     * passam intactas). Suficiente para o bibliotecario reconhecer o ano/serie
     * sem expor o identificador completo.
     */
    private static String mascarar(String matricula) {
        if (matricula == null || matricula.length() <= 5) {
            return matricula;
        }
        return matricula.substring(0, 5) + "*".repeat(matricula.length() - 5);
    }
}
