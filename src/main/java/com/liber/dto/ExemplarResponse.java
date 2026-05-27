package com.liber.dto;

import com.liber.entity.Exemplar;
import com.liber.entity.SituacaoExemplar;
import java.time.Instant;

/** Exemplar fisico individual de um livro (codigo de tombamento + situacao). */
public record ExemplarResponse(
    Long id,
    String codigo,
    SituacaoExemplar situacao,
    Instant createdAt,
    Instant updatedAt
) {

    public static ExemplarResponse from(Exemplar e) {
        return new ExemplarResponse(
            e.getId(),
            e.getCodigo(),
            e.getSituacao(),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
