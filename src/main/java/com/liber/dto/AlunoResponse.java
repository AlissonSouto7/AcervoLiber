package com.liber.dto;

import com.liber.entity.Aluno;
import com.liber.util.Cpf;
import java.time.Instant;

public record AlunoResponse(
    Long id,
    /** CPF formatado (123.456.789-01). Em telas vistas por terceiros, use o factory mascarado. */
    String cpf,
    String nome,
    String turma,
    long livrosEmprestadosAtualmente,
    Instant createdAt,
    Instant updatedAt
) {

    /** Para uso do bibliotecario/admin — CPF completo formatado. */
    public static AlunoResponse from(Aluno aluno, long livrosEmprestadosAtualmente) {
        return new AlunoResponse(
            aluno.getId(),
            Cpf.format(aluno.getCpf()),
            aluno.getNome(),
            aluno.getTurma(),
            livrosEmprestadosAtualmente,
            aluno.getCreatedAt(),
            aluno.getUpdatedAt()
        );
    }

    /** LGPD: CPF mascarado (123.***.***-01) em telas com exposicao de terceiros. */
    public static AlunoResponse fromMascarado(Aluno aluno, long livrosEmprestadosAtualmente) {
        return new AlunoResponse(
            aluno.getId(),
            Cpf.mask(aluno.getCpf()),
            aluno.getNome(),
            aluno.getTurma(),
            livrosEmprestadosAtualmente,
            aluno.getCreatedAt(),
            aluno.getUpdatedAt()
        );
    }
}
