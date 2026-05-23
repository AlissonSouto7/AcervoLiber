package com.liber.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.liber.util.Isbn;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Resolve a URL da capa de um livro.
 *
 * <p>Por que no backend (e nao no navegador): chamar APIs de capa direto do
 * frontend, com varios livros na tela, dispara muitas requisicoes simultaneas
 * e estoura o rate limit. Aqui a consulta e cacheada (cada livro consultado uma
 * vez) e o resultado e persistido em {@code livros.capa_url} — o frontend so le
 * a URL pronta.
 *
 * <p>Estrategia de resolucao, em ordem (para na primeira que achar capa):
 * <ol>
 *   <li><b>Google Books por ISBN</b> — a edicao exata; melhor match. So roda com
 *       chave de API ({@code app.capas.google-books-api-key}).</li>
 *   <li><b>Google Books por titulo + autor</b> — encontra a OBRA mesmo quando o
 *       ISBN exato nao esta na base (caso comum com edicoes nacionais). Para uma
 *       capa de catalogo, qualquer edicao do mesmo livro serve.</li>
 *   <li><b>Open Library por ISBN</b> — fallback sem chave; cobre uma parte dos
 *       livros nacionais.</li>
 * </ol>
 *
 * O que nenhuma fonte tiver fica sem capa (o frontend desenha a capa gerada).
 */
@Service
@Slf4j
public class CapaService {

    private static final String GOOGLE_BOOKS_URL =
        "https://www.googleapis.com/books/v1/volumes?country=BR&maxResults=5&q=";
    private static final String OPEN_LIBRARY_COVER_URL =
        "https://covers.openlibrary.org/b/isbn/%s-L.jpg?default=false";

    /**
     * Hosts cuja resposta de capa o servidor aceita persistir em {@code livros.capa_url}.
     * Tudo fora dessa lista e rejeitado — evita que uma URL adversaria devolvida
     * por uma fonte comprometida (ou via redirect controlado) acabe no
     * {@code <img src>} de todos os usuarios. Cobre vetor de XSS armazenado via
     * {@code data:image/svg+xml,<svg onload=...>}.
     */
    private static final Set<String> HOSTS_CAPA_PERMITIDOS = Set.of(
        "books.google.com",
        "books.googleusercontent.com",
        "lh3.googleusercontent.com",
        "covers.openlibrary.org"
    );

    /** Cap do cache em memoria — evita crescimento ilimitado por consultas autenticadas. */
    private static final int CACHE_MAX_ENTRIES = 10_000;

    /**
     * Cache em memoria com cap (LRU): chave do livro -> URL da capa.
     * String vazia = "ja consultado, sem capa em nenhuma fonte".
     * Usa LinkedHashMap em modo acesso para evictar o mais antigo ao passar do cap.
     */
    private final Map<String, String> cache = Collections.synchronizedMap(
        new LinkedHashMap<String, String>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > CACHE_MAX_ENTRIES;
            }
        }
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(4))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private final ObjectMapper objectMapper;
    private final String googleBooksApiKey;

    public CapaService(ObjectMapper objectMapper,
                       @Value("${app.capas.google-books-api-key:}") String googleBooksApiKey) {
        this.objectMapper = objectMapper;
        this.googleBooksApiKey = googleBooksApiKey == null ? "" : googleBooksApiKey.trim();
    }

    /**
     * Retorna a URL da capa do livro, ou {@code null} se nenhuma fonte tiver
     * capa. Nunca lanca excecao — falhas transitorias (rede, timeout, 429) viram
     * {@code null} e nao sao cacheadas, permitindo nova tentativa depois (ver
     * CapaBackfillJob).
     *
     * @param isbn   ISBN do livro (pode ser nulo/vazio)
     * @param titulo titulo do livro (usado no fallback por titulo+autor)
     * @param autor  autor do livro (usado no fallback por titulo+autor)
     */
    public String resolverCapa(String isbn, String titulo, String autor) {
        String chave = chaveCache(isbn, titulo, autor);
        if (chave.isEmpty()) {
            return null;
        }
        String cached = cache.get(chave);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }

        boolean erroTransitorio = false;
        String isbnNorm = Isbn.normalize(isbn);
        if (isbnNorm == null) {
            isbnNorm = "";
        }

        if (!googleBooksApiKey.isEmpty()) {
            // 1) Google Books pelo ISBN — edicao exata.
            if (!isbnNorm.isEmpty()) {
                try {
                    String url = consultarGoogleBooks("isbn:" + isbnNorm);
                    if (url != null) {
                        cache.put(chave, url);
                        return url;
                    }
                } catch (Exception e) {
                    log.warn("Google Books (ISBN {}) falhou: {}", isbnNorm, e.toString());
                    erroTransitorio = true;
                }
            }
            // 2) Google Books por titulo + autor — encontra a obra.
            if (temTexto(titulo)) {
                try {
                    String query = "intitle:" + titulo
                        + (temTexto(autor) ? " inauthor:" + autor : "");
                    String url = consultarGoogleBooks(query);
                    if (url != null) {
                        cache.put(chave, url);
                        return url;
                    }
                } catch (Exception e) {
                    log.warn("Google Books (titulo '{}') falhou: {}", titulo, e.toString());
                    erroTransitorio = true;
                }
            }
        }

        // 3) Open Library pelo ISBN — fallback sem chave.
        if (!isbnNorm.isEmpty()) {
            try {
                String url = consultarOpenLibrary(isbnNorm);
                if (url != null) {
                    cache.put(chave, url);
                    return url;
                }
            } catch (Exception e) {
                log.warn("Open Library (ISBN {}) falhou: {}", isbnNorm, e.toString());
                erroTransitorio = true;
            }
        }

        // Nada encontrado: cacheia o negativo so se nao houve erro transitorio
        // (assim um 429/timeout permite nova tentativa no proximo backfill).
        if (!erroTransitorio) {
            cache.put(chave, "");
        }
        return null;
    }

    /**
     * Consulta a Google Books API com uma expressao de busca livre (ex.:
     * {@code "isbn:9788535910663"} ou {@code "intitle:Dom Casmurro inauthor:Machado de Assis"}).
     * Retorna a URL da capa do primeiro resultado que tiver imagem, ou null.
     */
    private String consultarGoogleBooks(String query) throws Exception {
        String url = GOOGLE_BOOKS_URL + URLEncoder.encode(query, StandardCharsets.UTF_8)
            + "&key=" + URLEncoder.encode(googleBooksApiKey, StandardCharsets.UTF_8);

        HttpResponse<String> resp = httpClient.send(
            HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(6))
                .header("Accept", "application/json")
                .GET().build(),
            HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() == 429) {
            throw new IllegalStateException("Google Books HTTP 429 (rate limit)");
        }
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("Google Books HTTP " + resp.statusCode());
        }
        GoogleBooksResponse dados = objectMapper.readValue(resp.body(), GoogleBooksResponse.class);
        String capa = dados.primeiraImagem();
        return capa == null ? null : tratarUrlGoogle(capa);
    }

    /**
     * Consulta a Open Library. Como o endpoint usa {@code default=false}, ele
     * responde 200 quando a capa existe e 404 quando nao — entao basta checar o
     * status. Retorna a URL se existir, null se nao existir (404).
     */
    private String consultarOpenLibrary(String isbn) throws Exception {
        String url = OPEN_LIBRARY_COVER_URL.formatted(isbn);

        HttpResponse<Void> resp = httpClient.send(
            HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(6))
                .GET().build(),
            HttpResponse.BodyHandlers.discarding());

        if (resp.statusCode() == 200) {
            // Mesma allowlist da Google — defesa em profundidade caso o template
            // mude ou um redirect interno aponte para outro host.
            return urlSegura(url);
        }
        if (resp.statusCode() == 404) {
            return null; // negativo genuino — a Open Library nao tem a capa
        }
        throw new IllegalStateException("Open Library HTTP " + resp.statusCode());
    }

    /** Chave de cache: o ISBN normalizado, ou "titulo|autor" se nao houver ISBN. */
    private static String chaveCache(String isbn, String titulo, String autor) {
        String isbnNorm = Isbn.normalize(isbn);
        if (isbnNorm != null) {
            return isbnNorm;
        }
        if (temTexto(titulo)) {
            return (titulo + "|" + (autor == null ? "" : autor)).toLowerCase().trim();
        }
        return "";
    }

    private static boolean temTexto(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Ajusta a URL devolvida pela Google:
     *  - http -> https (evita bloqueio de "mixed content" no navegador)
     *  - remove &edge=curl (efeito de pagina dobrada)
     *  - zoom=1 -> zoom=2 (imagem com o dobro da resolucao)
     *
     * <p>Retorna {@code null} se a URL nao for {@code https} ou se o host nao
     * estiver em {@link #HOSTS_CAPA_PERMITIDOS} — barra schemes adversarios
     * ({@code data:}, {@code javascript:}) e hosts arbitrarios que poderiam
     * acabar como {@code <img src>} no frontend de todos os usuarios.
     */
    private static String tratarUrlGoogle(String url) {
        String ajustada = url
            .replaceFirst("^http://", "https://")
            .replace("&edge=curl", "")
            .replace("&zoom=1&", "&zoom=2&");
        return urlSegura(ajustada);
    }

    /**
     * Aceita apenas URLs absolutas {@code https://} cujo host esta em
     * {@link #HOSTS_CAPA_PERMITIDOS}. Qualquer outra coisa (URL relativa,
     * {@code data:}, {@code javascript:}, host arbitrario) vira {@code null}.
     */
    static String urlSegura(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(scheme) || host == null) {
                log.warn("URL de capa rejeitada (scheme/host invalido): {}", url);
                return null;
            }
            String hostLower = host.toLowerCase(Locale.ROOT);
            if (!HOSTS_CAPA_PERMITIDOS.contains(hostLower)) {
                log.warn("URL de capa rejeitada (host fora da allowlist): host={}", hostLower);
                return null;
            }
            return url;
        } catch (IllegalArgumentException e) {
            log.warn("URL de capa rejeitada (URI invalida): {}", e.getMessage());
            return null;
        }
    }

    // --- Estrutura minima do JSON da Google Books (so os campos que interessam) ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GoogleBooksResponse(List<Item> items) {

        /** URL da imagem do primeiro resultado que tiver capa, ou null. */
        String primeiraImagem() {
            if (items == null) {
                return null;
            }
            for (Item item : items) {
                if (item.volumeInfo() == null || item.volumeInfo().imageLinks() == null) {
                    continue;
                }
                ImageLinks links = item.volumeInfo().imageLinks();
                String url = links.thumbnail() != null ? links.thumbnail() : links.smallThumbnail();
                if (url != null) {
                    return url;
                }
            }
            return null;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Item(VolumeInfo volumeInfo) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        record VolumeInfo(ImageLinks imageLinks) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        record ImageLinks(String thumbnail, String smallThumbnail) {}
    }
}
