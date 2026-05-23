package com.liber.service;

import com.liber.entity.Livro;
import com.liber.repository.LivroRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Preenche a capa de livros que tem ISBN mas ainda nao tiveram a capa resolvida.
 *
 * <p>Cobre dois casos:
 * <ul>
 *   <li>dados de exemplo (seed), cadastrados sem capa;</li>
 *   <li>livros cuja resolucao no cadastro falhou (ex.: Google Books fora do ar
 *       ou 429 momentaneo).</li>
 * </ul>
 *
 * <p>Roda ~40s apos o boot e a cada 6h. As chamadas a Google Books sao
 * <b>espacadas</b> (uma capa de cada vez, com pausa) para nunca estourar o
 * rate limit — o oposto do que acontecia quando o navegador disparava todas
 * as requisicoes de uma vez.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CapaBackfillJob {

    /** Pausa entre consultas a Google Books — espaca as chamadas, evita 429. */
    private static final long PAUSA_ENTRE_CONSULTAS_MS = 300;

    private final LivroRepository livroRepository;
    private final LivroService livroService;
    private final CapaService capaService;

    /** Evita que duas execucoes se sobreponham. */
    private final AtomicBoolean rodando = new AtomicBoolean(false);

    @Scheduled(initialDelay = 40_000, fixedDelay = 6 * 60 * 60 * 1000)
    public void preencherCapasFaltantes() {
        if (!rodando.compareAndSet(false, true)) {
            return; // ja ha uma execucao em andamento
        }
        try {
            List<Livro> semCapa = livroRepository.findByIsbnIsNotNullAndCapaUrlIsNull();
            if (semCapa.isEmpty()) {
                return;
            }
            log.info("CapaBackfill: resolvendo capa de {} livro(s)...", semCapa.size());

            int preenchidas = 0;
            for (Livro livro : semCapa) {
                String url = capaService.resolverCapa(
                    livro.getIsbn(), livro.getTitulo(), livro.getAutor());
                if (url != null) {
                    livroService.definirCapa(livro.getId(), url);
                    preenchidas++;
                }
                try {
                    Thread.sleep(PAUSA_ENTRE_CONSULTAS_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            log.info("CapaBackfill: {} de {} capas preenchidas.", preenchidas, semCapa.size());
        } finally {
            rodando.set(false);
        }
    }
}
