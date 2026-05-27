package com.liber.dto;

import com.liber.entity.Aluno;
import com.liber.util.Cpf;

public record AlunoResumoDTO(
    Long id,
    String cpf,
    String nome,
    String turma
) {

    public static AlunoResumoDTO from(Aluno aluno) {
        return new AlunoResumoDTO(aluno.getId(), Cpf.format(aluno.getCpf()), aluno.getNome(), aluno.getTurma());
    }

    /**
     * Variante para telas administrativas (Reservas Pendentes, Emprestimos Ativos)
     * visiveis a quem passar atras do balcao. Mantem nome e turma intactos (o BIB
     * precisa identificar o aluno), mascara o CPF — identificador unico de menor
     * (LGPD §14).
     */
    public static AlunoResumoDTO mascarado(Aluno aluno) {
        return new AlunoResumoDTO(aluno.getId(), Cpf.mask(aluno.getCpf()),
            aluno.getNome(), aluno.getTurma());
    }
}
