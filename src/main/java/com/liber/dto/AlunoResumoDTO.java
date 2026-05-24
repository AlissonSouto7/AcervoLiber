package com.liber.dto;

import com.liber.entity.Aluno;

public record AlunoResumoDTO(
    Long id,
    String matricula,
    String nome,
    String turma
) {

    public static AlunoResumoDTO from(Aluno aluno) {
        return new AlunoResumoDTO(aluno.getId(), aluno.getMatricula(), aluno.getNome(), aluno.getTurma());
    }

    /**
     * Variante para telas administrativas (Reservas Pendentes, Emprestimos Ativos)
     * visiveis a quem passar atras do balcao. Mantem nome e turma intactos (o BIB
     * precisa identificar o aluno), mascara a matricula — identificador unico que
     * segue o aluno por toda a vida escolar (LGPD §14, dados de menores).
     * Padrao igual ao {@link DashboardAlertaDTO}.
     */
    public static AlunoResumoDTO mascarado(Aluno aluno) {
        return new AlunoResumoDTO(aluno.getId(), mascararMatricula(aluno.getMatricula()),
            aluno.getNome(), aluno.getTurma());
    }

    private static String mascararMatricula(String matricula) {
        if (matricula == null || matricula.length() <= 5) {
            return matricula;
        }
        return matricula.substring(0, 5) + "*".repeat(matricula.length() - 5);
    }
}
