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
            LivroResumoDTO.from(reserva.getLivro()),
            AlunoResumoDTO.from(reserva.getAluno()),
            reserva.getStatus(),
            reserva.getDataReserva(),
            reserva.getDataExpiracao()
        );
    }

    /** Variante para fila do bibliotecario — matricula do aluno mascarada (LGPD). */
    public static ReservaResponse fromMascarado(Reserva reserva) {
        return new ReservaResponse(
            reserva.getId(),
            LivroResumoDTO.from(reserva.getLivro()),
            AlunoResumoDTO.mascarado(reserva.getAluno()),
            reserva.getStatus(),
            reserva.getDataReserva(),
            reserva.getDataExpiracao()
        );
    }
}
