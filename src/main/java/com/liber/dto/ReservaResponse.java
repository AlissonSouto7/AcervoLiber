package com.liber.dto;

import com.liber.entity.Reserva;
import com.liber.entity.StatusReserva;
import java.time.LocalDate;

public record ReservaResponse(
    Long id,
    LivroResumoDTO livro,
    AlunoResumoDTO aluno,
    StatusReserva status,
    LocalDate dataReserva,
    LocalDate dataExpiracao
) {

    public static ReservaResponse from(Reserva reserva) {
        return new ReservaResponse(
            reserva.getId(),
            livroResumo(reserva),
            AlunoResumoDTO.from(reserva.getAluno()),
            reserva.getStatus(),
            reserva.getDataReserva(),
            reserva.getDataExpiracao()
        );
    }

    /** Variante para fila do bibliotecario — CPF do aluno mascarado (LGPD). */
    public static ReservaResponse fromMascarado(Reserva reserva) {
        return new ReservaResponse(
            reserva.getId(),
            livroResumo(reserva),
            AlunoResumoDTO.mascarado(reserva.getAluno()),
            reserva.getStatus(),
            reserva.getDataReserva(),
            reserva.getDataExpiracao()
        );
    }

    /**
     * Inclui o codigo do exemplar segurado pela reserva quando ele esta
     * atribuido — o bibliotecario precisa saber qual copia fisica entregar
     * pro aluno na confirmacao.
     */
    private static LivroResumoDTO livroResumo(Reserva reserva) {
        return reserva.getExemplar() == null
            ? LivroResumoDTO.from(reserva.getLivro())
            : LivroResumoDTO.from(reserva.getLivro(), reserva.getExemplar());
    }
}
