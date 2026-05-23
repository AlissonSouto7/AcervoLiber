package com.liber.dto;

import com.liber.entity.Emprestimo;
import com.liber.entity.SituacaoEmprestimo;
import java.time.LocalDate;

public record EmprestimoResponse(
    Long id,
    LivroResumoDTO livro,
    AlunoResumoDTO aluno,
    LocalDate dataEmprestimo,
    Integer prazoDias,
    LocalDate dataDevolucaoPrevista,
    LocalDate dataDevolucaoEfetiva,
    SituacaoEmprestimo situacao,
    StatusUrgencia statusUrgencia
) {

    public static EmprestimoResponse from(Emprestimo emprestimo, LocalDate hoje) {
        return new EmprestimoResponse(
            emprestimo.getId(),
            LivroResumoDTO.from(emprestimo.getLivro()),
            AlunoResumoDTO.from(emprestimo.getAluno()),
            emprestimo.getDataEmprestimo(),
            emprestimo.getPrazoDias(),
            emprestimo.getDataDevolucaoPrevista(),
            emprestimo.getDataDevolucaoEfetiva(),
            emprestimo.getSituacao(),
            StatusUrgencia.from(emprestimo, hoje)
        );
    }
}
