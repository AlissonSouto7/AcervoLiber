package com.liber.util;

/**
 * Normalizacao canonica de ISBN.
 *
 * <p>Usada para validar duplicidade, gravar no banco e calcular chave de cache de
 * capa — todos sob a mesma forma, para "978-8535914849" e "9788535914849"
 * serem reconhecidos como o mesmo livro.
 */
public final class Isbn {

    private Isbn() {}

    /**
     * Retorna o ISBN apenas com digitos e 'X' (maiusculo), sem hifens, espacos
     * ou outros separadores. Retorna {@code null} se a entrada for nula ou
     * resultar em string vazia apos a normalizacao (assim, "  -  " devolve null).
     */
    public static String normalize(String isbn) {
        if (isbn == null) {
            return null;
        }
        String limpo = isbn.replaceAll("[^0-9Xx]", "").toUpperCase();
        return limpo.isEmpty() ? null : limpo;
    }
}
