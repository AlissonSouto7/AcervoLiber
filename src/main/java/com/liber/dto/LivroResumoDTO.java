package com.liber.dto;

import com.liber.entity.Exemplar;
import com.liber.entity.Livro;

/**
 * Resumo de livro pra listagens (emprestimos, reservas, historico). Inclui o
 * codigo do exemplar quando o resumo se refere a uma copia especifica (caso
 * do emprestimo) — assim o bibliotecario sabe exatamente qual etiqueta esta
 * com o aluno, nao apenas o titulo.
 */
public record LivroResumoDTO(
    Long id,
    String titulo,
    String autor,
    /** ID do exemplar fisico envolvido. Null em contextos puramente bibliograficos. */
    Long exemplarId,
    /** Codigo de tombamento do exemplar (ex.: LIB-00042). Null se nao aplicavel. */
    String exemplarCodigo
) {

    /** Pra contextos onde nao ha exemplar especifico (catalogo, reserva por titulo). */
    public static LivroResumoDTO from(Livro livro) {
        return new LivroResumoDTO(livro.getId(), livro.getTitulo(), livro.getAutor(), null, null);
    }

    /** Pra emprestimos e devolucoes — inclui o codigo do exemplar emprestado. */
    public static LivroResumoDTO from(Livro livro, Exemplar exemplar) {
        return new LivroResumoDTO(
            livro.getId(), livro.getTitulo(), livro.getAutor(),
            exemplar.getId(), exemplar.getCodigo());
    }
}
