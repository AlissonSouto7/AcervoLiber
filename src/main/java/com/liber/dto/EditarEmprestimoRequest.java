package com.liber.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;

/**
 * Payload de edicao de empréstimo. Ambos os campos opcionais — apenas os
 * informados sao alterados. Cobre correcao de lancamento errado pelo
 * bibliotecario (prazo digitado errado, data de empréstimo digitada errada).
 *
 * <p>NAO permite trocar livro nem aluno — pra esse caso, usar DELETE (cancelar)
 * + POST novo. Trocar livro/aluno requer ajustes de estoque que sao mais
 * limpos via cancelamento explicito.
 */
public record EditarEmprestimoRequest(

    LocalDate dataEmprestimo,

    @Min(value = 1, message = "Prazo deve ser ao menos 1 dia")
    @Max(value = 3650, message = "Prazo nao pode exceder 3650 dias")
    Integer prazoDias
) {

    public boolean isVazio() {
        return dataEmprestimo == null && prazoDias == null;
    }
}
