package com.liber.service;

import com.liber.dto.LivroRequest;
import com.liber.dto.LivroResponse;
import com.liber.entity.Livro;
import com.liber.entity.LivroCapa;
import com.liber.entity.SituacaoEmprestimo;
import com.liber.entity.StatusReserva;
import com.liber.exception.BusinessException;
import com.liber.exception.RegraEmprestimoException;
import com.liber.exception.ResourceNotFoundException;
import com.liber.repository.EmprestimoRepository;
import com.liber.repository.LivroCapaRepository;
import com.liber.repository.LivroRepository;
import com.liber.repository.ReservaRepository;
import com.liber.util.Isbn;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class LivroService {

    /** Formatos de imagem aceitos no upload de capa. */
    private static final Set<String> TIPOS_IMAGEM_PERMITIDOS =
        Set.of("image/jpeg", "image/png", "image/webp");
    /** Tamanho maximo da capa enviada: 2 MB. */
    private static final long TAMANHO_MAX_CAPA = 2L * 1024 * 1024;

    private final LivroRepository livroRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final ReservaRepository reservaRepository;
    private final LivroCapaRepository livroCapaRepository;
    private final CapaService capaService;
    private final Clock clock;

    public Page<LivroResponse> listar(String termo, Pageable pageable) {
        return livroRepository.buscar(termo, pageable).map(LivroResponse::from);
    }

    public LivroResponse buscarPorId(Long id) {
        return LivroResponse.from(carregar(id));
    }

    @Transactional
    public LivroResponse cadastrar(LivroRequest req) {
        // Normaliza ANTES de checar duplicidade — "978-8535914849" e "9788535914849"
        // sao a mesma edicao; sem isso a checagem passava e a unique constraint
        // do DB so disparava no flush (HTTP 500 generico em vez de 422 amigavel).
        String isbn = Isbn.normalize(req.isbn());
        if (isbn != null && livroRepository.existsByIsbn(isbn)) {
            throw new BusinessException("ISBN ja cadastrado: " + isbn);
        }

        // A capa nao e resolvida sincronamente aqui — o CapaBackfillJob preenche
        // depois. Isso evita segurar a conexao do DB por ate ~18s esperando o
        // Google Books / Open Library quando a internet ou esses servicos estao
        // lentos.
        Livro livro = Livro.builder()
            .titulo(req.titulo())
            .autor(req.autor())
            .isbn(isbn)
            .ano(req.ano())
            .quantidadeExemplares(req.quantidadeExemplares())
            .quantidadeDisponivel(req.quantidadeExemplares())
            .capaUrl(null)
            .build();

        Livro salvo = livroRepository.save(livro);
        log.info("Livro cadastrado id={} titulo='{}'", salvo.getId(), salvo.getTitulo());
        return LivroResponse.from(salvo);
    }

    @Transactional
    public LivroResponse atualizar(Long id, LivroRequest req) {
        Livro livro = carregar(id);

        String isbnAntigo = livro.getIsbn();
        String tituloAntigo = livro.getTitulo();
        String autorAntigo = livro.getAutor();
        String isbnNovo = Isbn.normalize(req.isbn());
        if (isbnNovo != null && !isbnNovo.equals(livro.getIsbn())
                && livroRepository.existsByIsbn(isbnNovo)) {
            throw new BusinessException("ISBN ja cadastrado: " + isbnNovo);
        }

        // Exemplares "em uso" = emprestimos ATIVOS + reservas PENDENTES. As reservas
        // pendentes tambem seguram um exemplar (decremento atomico ao reservar), entao
        // precisam entrar na conta — senao o estoque disponivel diverge e exemplares
        // somem ou aparecem indevidamente apos cancelar/recusar reservas.
        long ativos = emprestimoRepository.countByLivroIdAndSituacao(id, SituacaoEmprestimo.ATIVO);
        long reservasPendentes = reservaRepository.countByLivroIdAndStatus(id, StatusReserva.PENDENTE);
        long emUso = ativos + reservasPendentes;
        if (req.quantidadeExemplares() < emUso) {
            throw new RegraEmprestimoException(
                ("Total de exemplares (%d) nao pode ser menor que os exemplares em uso "
                    + "— emprestimos ativos + reservas pendentes (%d)")
                    .formatted(req.quantidadeExemplares(), emUso));
        }

        livro.setTitulo(req.titulo());
        livro.setAutor(req.autor());
        livro.setIsbn(isbnNovo);
        livro.setAno(req.ano());
        livro.setQuantidadeExemplares(req.quantidadeExemplares());
        // Calculado direto a partir dos exemplares em uso — independente do estado
        // anterior de quantidadeDisponivel (robusto contra eventuais drifts).
        livro.setQuantidadeDisponivel(req.quantidadeExemplares() - (int) emUso);

        // ISBN, titulo ou autor mudou -> capa automatica fica obsoleta. Limpa para
        // o CapaBackfillJob re-resolver em background; nao resolvemos sincronamente
        // aqui para evitar segurar conexao do DB esperando o Google Books / Open
        // Library. Capa enviada manualmente nunca e sobrescrita.
        boolean dadosDaCapaMudaram = !Objects.equals(isbnAntigo, isbnNovo)
            || !Objects.equals(tituloAntigo, req.titulo())
            || !Objects.equals(autorAntigo, req.autor());
        if (dadosDaCapaMudaram && !livro.isCapaManual()) {
            livro.setCapaUrl(null);
        }

        log.info("Livro atualizado id={}", id);
        return LivroResponse.from(livroRepository.save(livro));
    }

    @Transactional
    public void remover(Long id) {
        if (!livroRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Livro", id);
        }
        if (emprestimoRepository.existsByLivroId(id)) {
            throw new BusinessException(
                "Nao e possivel remover livro com historico de emprestimos. Mantenha-o para preservar o historico.");
        }
        // Reservas PENDENTE e CONFIRMADA seguram um exemplar — deletar agora deixaria
        // a reserva orfa (FK CASCADE removeria sem aviso, descalibrando o estoque
        // exibido pro aluno).
        if (reservaRepository.countByLivroIdAndStatus(id, StatusReserva.PENDENTE) > 0
                || reservaRepository.countByLivroIdAndStatus(id, StatusReserva.CONFIRMADA) > 0) {
            throw new BusinessException(
                "Nao e possivel remover livro com reservas pendentes ou confirmadas. Cancele as reservas antes.");
        }
        livroRepository.deleteById(id);
        log.info("Livro removido id={}", id);
    }

    Livro carregar(Long id) {
        return livroRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Livro", id));
    }

    /**
     * Resolve a URL da capa sem persistir nada — usado pelo preview do formulario
     * (o livro ainda nem existe). Retorna null se nao houver capa.
     */
    public String resolverCapa(String isbn, String titulo, String autor) {
        return capaService.resolverCapa(isbn, titulo, autor);
    }

    /**
     * Define a capa automatica de um livro. Usado pelo CapaBackfillJob para
     * preencher capas faltantes. Nao toca em livros com capa manual.
     */
    @Transactional
    public void definirCapa(Long id, String capaUrl) {
        livroRepository.findById(id).ifPresent(livro -> {
            if (!livro.isCapaManual()) {
                livro.setCapaUrl(capaUrl);
                livroRepository.save(livro);
            }
        });
    }

    /**
     * Substitui a capa do livro por uma imagem enviada manualmente. A partir daqui
     * a resolucao automatica nao mexe mais na capa (ate ela ser removida).
     */
    @Transactional
    public LivroResponse enviarCapa(Long id, byte[] dados, String contentType) {
        Livro livro = carregar(id);
        if (dados == null || dados.length == 0) {
            throw new BusinessException("Arquivo de imagem vazio.");
        }
        if (dados.length > TAMANHO_MAX_CAPA) {
            throw new BusinessException("Imagem muito grande. O tamanho maximo e 2 MB.");
        }
        String tipo = contentType == null ? "" : contentType.toLowerCase();
        if (!TIPOS_IMAGEM_PERMITIDOS.contains(tipo)) {
            throw new BusinessException("Formato invalido. Envie uma imagem JPG, PNG ou WEBP.");
        }
        // O Content-Type acima e informado pelo cliente e trivialmente forjavel.
        // A validacao que vale e a dos magic bytes do conteudo real — e o tipo
        // armazenado/servido depois e o detectado, nunca o informado pelo cliente.
        String tipoReal = detectarTipoImagem(dados);
        if (tipoReal == null) {
            throw new BusinessException(
                "O arquivo enviado nao e uma imagem valida (JPG, PNG ou WEBP).");
        }

        LivroCapa capa = livroCapaRepository.findById(id).orElseGet(LivroCapa::new);
        capa.setLivroId(id);
        capa.setImagem(dados);
        capa.setContentType(tipoReal);
        capa.setAtualizadoEm(Instant.now(clock));
        livroCapaRepository.save(capa);

        // O sufixo ?v= força o navegador a recarregar a imagem apos uma troca.
        livro.setCapaUrl("/api/v1/livros/" + id + "/capa-imagem?v=" + System.currentTimeMillis());
        livro.setCapaManual(true);
        Livro salvo = livroRepository.save(livro);
        log.info("Capa manual enviada para livro id={} ({} bytes)", id, dados.length);
        return LivroResponse.from(salvo);
    }

    /** Imagem da capa enviada manualmente — para o endpoint que serve a imagem. */
    @Transactional(readOnly = true)
    public LivroCapa lerCapaImagem(Long id) {
        return livroCapaRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Capa do livro", id));
    }

    /**
     * Remove a capa manual e volta para a capa automatica (Google Books / Open
     * Library), re-resolvendo na hora.
     */
    @Transactional
    public LivroResponse removerCapaManual(Long id) {
        Livro livro = carregar(id);
        if (livroCapaRepository.existsById(id)) {
            livroCapaRepository.deleteById(id);
        }
        livro.setCapaManual(false);
        livro.setCapaUrl(capaService.resolverCapa(livro.getIsbn(), livro.getTitulo(), livro.getAutor()));
        Livro salvo = livroRepository.save(livro);
        log.info("Capa manual removida do livro id={} — voltou para a automatica", id);
        return LivroResponse.from(salvo);
    }

    /**
     * Detecta o tipo de imagem pelos magic bytes do conteudo — sem confiar no
     * Content-Type informado pelo cliente. Retorna o mime type ou {@code null}
     * se o arquivo nao for um JPG, PNG ou WEBP genuino.
     */
    private static String detectarTipoImagem(byte[] d) {
        if (d.length >= 3
                && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (d.length >= 8
                && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && (d[4] & 0xFF) == 0x0D && (d[5] & 0xFF) == 0x0A
                && (d[6] & 0xFF) == 0x1A && (d[7] & 0xFF) == 0x0A) {
            return "image/png";
        }
        if (d.length >= 12
                && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}
