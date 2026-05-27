package com.liber.service;

import com.liber.dto.ExemplarRequest;
import com.liber.dto.ExemplarResponse;
import com.liber.entity.Exemplar;
import com.liber.entity.Livro;
import com.liber.entity.SituacaoExemplar;
import com.liber.exception.BusinessException;
import com.liber.exception.ResourceNotFoundException;
import com.liber.repository.EmprestimoRepository;
import com.liber.repository.ExemplarRepository;
import com.liber.repository.LivroRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operacoes sobre exemplares individuais de um livro: listar, adicionar avulso,
 * renomear codigo, marcar como extraviado, reativar e remover.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ExemplarService {

    private final ExemplarRepository exemplarRepository;
    private final LivroRepository livroRepository;
    private final EmprestimoRepository emprestimoRepository;

    /** Lista os exemplares de um livro em ordem de codigo. */
    public List<ExemplarResponse> listarDoLivro(Long livroId) {
        if (!livroRepository.existsById(livroId)) {
            throw ResourceNotFoundException.of("Livro", livroId);
        }
        return exemplarRepository.findByLivroIdOrderByCodigoAsc(livroId)
            .stream().map(ExemplarResponse::from).toList();
    }

    /** Adiciona 1 exemplar ao livro. Codigo opcional — se vazio, usa default da sequence. */
    @Transactional
    public ExemplarResponse adicionar(Long livroId, ExemplarRequest req) {
        Livro livro = livroRepository.findById(livroId)
            .orElseThrow(() -> ResourceNotFoundException.of("Livro", livroId));
        String codigo = normalizarCodigo(req == null ? null : req.codigo());
        if (codigo == null) {
            codigo = exemplarRepository.proximoCodigoPadrao();
        } else if (exemplarRepository.existsByCodigo(codigo)) {
            throw new BusinessException("Ja existe um exemplar com o codigo: " + codigo);
        }
        Exemplar e = exemplarRepository.save(Exemplar.builder()
            .livro(livro)
            .codigo(codigo)
            .situacao(SituacaoExemplar.DISPONIVEL)
            .build());
        log.info("Exemplar criado id={} codigo='{}' livro_id={}", e.getId(), codigo, livroId);
        return ExemplarResponse.from(e);
    }

    /** Renomeia o codigo do exemplar (pra casar com etiqueta fisica). */
    @Transactional
    public ExemplarResponse renomear(Long exemplarId, ExemplarRequest req) {
        String novoCodigo = normalizarCodigo(req == null ? null : req.codigo());
        if (novoCodigo == null) {
            throw new BusinessException("Informe o novo codigo do exemplar.");
        }
        Exemplar e = carregar(exemplarId);
        if (!novoCodigo.equals(e.getCodigo()) && exemplarRepository.existsByCodigo(novoCodigo)) {
            throw new BusinessException("Ja existe um exemplar com o codigo: " + novoCodigo);
        }
        e.setCodigo(novoCodigo);
        log.info("Exemplar id={} renomeado para codigo='{}'", exemplarId, novoCodigo);
        return ExemplarResponse.from(exemplarRepository.save(e));
    }

    /**
     * Marca o exemplar como EXTRAVIADO. So vale se ele estiver DISPONIVEL —
     * exemplares EMPRESTADO/RESERVADO precisam ser devolvidos/cancelados antes.
     */
    @Transactional
    public ExemplarResponse marcarExtraviado(Long exemplarId) {
        Exemplar e = carregar(exemplarId);
        if (e.getSituacao() != SituacaoExemplar.DISPONIVEL
                && e.getSituacao() != SituacaoExemplar.EXTRAVIADO) {
            throw new BusinessException(
                "Nao e possivel marcar como extraviado um exemplar " + e.getSituacao()
                    + ". Devolva ou cancele a reserva antes.");
        }
        e.setSituacao(SituacaoExemplar.EXTRAVIADO);
        log.info("Exemplar id={} codigo='{}' marcado como EXTRAVIADO", exemplarId, e.getCodigo());
        return ExemplarResponse.from(exemplarRepository.save(e));
    }

    /** Reativa um exemplar extraviado (voltou a aparecer no acervo). */
    @Transactional
    public ExemplarResponse reativar(Long exemplarId) {
        Exemplar e = carregar(exemplarId);
        if (e.getSituacao() != SituacaoExemplar.EXTRAVIADO) {
            throw new BusinessException(
                "So e possivel reativar exemplar EXTRAVIADO (situacao atual: "
                    + e.getSituacao() + ").");
        }
        e.setSituacao(SituacaoExemplar.DISPONIVEL);
        log.info("Exemplar id={} codigo='{}' reativado (DISPONIVEL)", exemplarId, e.getCodigo());
        return ExemplarResponse.from(exemplarRepository.save(e));
    }

    /**
     * Remove um exemplar do acervo (apaga registro). So se ele estiver DISPONIVEL
     * e nao tiver historico de emprestimo. Caso contrario, prefira marcar como
     * EXTRAVIADO pra preservar o historico.
     */
    @Transactional
    public void remover(Long exemplarId) {
        Exemplar e = carregar(exemplarId);
        if (e.getSituacao() != SituacaoExemplar.DISPONIVEL) {
            throw new BusinessException(
                "So e possivel remover exemplar DISPONIVEL. Para extravio, use a acao apropriada.");
        }
        if (emprestimoRepository.existsByExemplarId(exemplarId)) {
            throw new BusinessException(
                "Nao e possivel remover exemplar com historico de emprestimos. "
                    + "Use a acao 'Marcar como extraviado' pra mante-lo no historico.");
        }
        exemplarRepository.deleteById(exemplarId);
        log.info("Exemplar id={} codigo='{}' removido", exemplarId, e.getCodigo());
    }

    Exemplar carregar(Long id) {
        return exemplarRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Exemplar", id));
    }

    private static String normalizarCodigo(String codigo) {
        if (codigo == null) return null;
        String t = codigo.trim();
        return t.isEmpty() ? null : t;
    }
}
