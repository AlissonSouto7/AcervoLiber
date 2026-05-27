package com.liber.entity;

/**
 * Estado fisico de cada exemplar (copia individual) de um livro.
 *
 * <p>Transicoes validas (impostas pelo servico):
 * <ul>
 *   <li>{@code DISPONIVEL} -&gt; {@code EMPRESTADO} (emprestimo direto no balcao)</li>
 *   <li>{@code DISPONIVEL} -&gt; {@code RESERVADO} (aluno reserva pelo portal)</li>
 *   <li>{@code RESERVADO}  -&gt; {@code EMPRESTADO} (bibliotecario confirma a reserva)</li>
 *   <li>{@code RESERVADO}  -&gt; {@code DISPONIVEL} (reserva cancelada / recusada / expirou)</li>
 *   <li>{@code EMPRESTADO} -&gt; {@code DISPONIVEL} (devolucao registrada)</li>
 *   <li>qualquer estado -&gt; {@code EXTRAVIADO}   (bibliotecario marca exemplar perdido)</li>
 *   <li>{@code EXTRAVIADO} -&gt; {@code DISPONIVEL} (livro apareceu, bibliotecario reativa)</li>
 * </ul>
 *
 * <p>EXTRAVIADO permanece visivel pra historico, mas e ignorado nas contagens
 * de "disponivel" do livro e nao pode ser emprestado/reservado.
 */
public enum SituacaoExemplar {
    DISPONIVEL,
    EMPRESTADO,
    RESERVADO,
    EXTRAVIADO
}
