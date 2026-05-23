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
}
