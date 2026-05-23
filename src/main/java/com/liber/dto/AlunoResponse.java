package com.liber.dto;

import com.liber.entity.Aluno;
import java.time.Instant;

public record AlunoResponse(
    Long id,
    String matricula,
    String nome,
    String turma,
    long livrosEmprestadosAtualmente,
    Instant createdAt,
    Instant updatedAt
) {

    public static AlunoResponse from(Aluno aluno, long livrosEmprestadosAtualmente) {
        return new AlunoResponse(
            aluno.getId(),
            aluno.getMatricula(),
            aluno.getNome(),
            aluno.getTurma(),
            livrosEmprestadosAtualmente,
            aluno.getCreatedAt(),
            aluno.getUpdatedAt()
        );
    }
}
