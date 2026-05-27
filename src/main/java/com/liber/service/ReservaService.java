package com.liber.service;

import com.liber.config.EmprestimoProperties;
import com.liber.config.ReservaProperties;
import com.liber.dto.ReservaResponse;
import com.liber.dto.ReservaResumoResponse;
import com.liber.entity.Aluno;
import com.liber.entity.Emprestimo;
import com.liber.entity.Exemplar;
import com.liber.entity.Livro;
import com.liber.entity.Reserva;
import com.liber.entity.SituacaoEmprestimo;
import com.liber.entity.SituacaoExemplar;
import com.liber.entity.StatusReserva;
import com.liber.entity.Usuario;
import com.liber.exception.BusinessException;
import com.liber.exception.EstoqueIndisponivelException;
import com.liber.exception.ResourceNotFoundException;
import com.liber.repository.AlunoRepository;
import com.liber.repository.EmprestimoRepository;
import com.liber.repository.ExemplarRepository;
import com.liber.repository.LivroRepository;
import com.liber.repository.ReservaRepository;
import com.liber.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reservas de livros pelos alunos.
 *
 * <p>Fluxo: o aluno reserva um livro disponivel (a reserva PENDENTE separa um
 * exemplar especifico — o primeiro DISPONIVEL — e marca-o como RESERVADO). O
 * bibliotecario confirma e o exemplar vira EMPRESTADO, ou recusa e ele volta
 * pra DISPONIVEL. Reservas nao retiradas expiram automaticamente pelo job.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final LivroRepository livroRepository;
    private final ExemplarRepository exemplarRepository;
    private final AlunoRepository alunoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final EmprestimoService emprestimoService;
    private final ReservaProperties reservaProps;
    private final EmprestimoProperties emprestimoProps;
    private final Clock clock;

    // ---------- Acoes do aluno ----------

    @Transactional
    public ReservaResponse reservar(String emailUsuario, Long livroId) {
        Long alunoId = resolverAlunoId(emailUsuario);

        Aluno aluno = alunoRepository.findByIdForUpdate(alunoId)
            .orElseThrow(() -> ResourceNotFoundException.of("Aluno", alunoId));

        if (reservaRepository.existsByAlunoIdAndLivroIdAndStatus(alunoId, livroId, StatusReserva.PENDENTE)) {
            throw new BusinessException("Voce ja tem uma reserva pendente para este livro.");
        }

        LocalDate hoje = LocalDate.now(clock);

        long atrasados = emprestimoRepository.countAtrasadosByAluno(alunoId, hoje);
        if (atrasados > 0) {
            throw new BusinessException(
                "Voce possui %d livro(s) em atraso. Devolva antes de reservar novos."
                    .formatted(atrasados));
        }

        long ativos = emprestimoRepository.countByAlunoIdAndSituacao(alunoId, SituacaoEmprestimo.ATIVO);
        long pendentes = reservaRepository.countByAlunoIdAndStatus(alunoId, StatusReserva.PENDENTE);
        int limite = emprestimoProps.limitePorAluno();
        if (ativos + pendentes >= limite) {
            throw new BusinessException(
                "Voce atingiu o limite de %d livros entre emprestimos e reservas.".formatted(limite));
        }

        // Reserva um exemplar especifico — o primeiro DISPONIVEL do livro, com
        // lock pessimista pra evitar race com outra reserva simultanea.
        Livro livro = livroRepository.findById(livroId)
            .orElseThrow(() -> ResourceNotFoundException.of("Livro", livroId));
        Exemplar exemplar = exemplarRepository.findPrimeiroDisponivelForUpdate(livroId)
            .orElseThrow(() -> new EstoqueIndisponivelException(livroId));
        exemplar.setSituacao(SituacaoExemplar.RESERVADO);
        exemplarRepository.save(exemplar);

        Reserva reserva = Reserva.builder()
            .livro(livro)
            .exemplar(exemplar)
            .aluno(aluno)
            .status(StatusReserva.PENDENTE)
            .dataReserva(hoje)
            .dataExpiracao(hoje.plusDays(reservaProps.validadeDias()))
            .build();
        Reserva salva = reservaRepository.save(reserva);
        log.info("Reserva criada id={} livro={} exemplar={} ({}) aluno_id={} validade={}",
            salva.getId(), livroId, exemplar.getId(), exemplar.getCodigo(),
            aluno.getId(), salva.getDataExpiracao());
        return ReservaResponse.from(salva);
    }

    public Page<ReservaResponse> listarMinhas(String emailUsuario, Pageable pageable) {
        Long alunoId = resolverAlunoId(emailUsuario);
        return reservaRepository.findByAlunoIdOrderByDataReservaDesc(alunoId, pageable)
            .map(ReservaResponse::from);
    }

    public ReservaResumoResponse resumoDoAluno(String emailUsuario) {
        Long alunoId = resolverAlunoId(emailUsuario);
        long ativos = emprestimoRepository.countByAlunoIdAndSituacao(alunoId, SituacaoEmprestimo.ATIVO);
        long pendentes = reservaRepository.countByAlunoIdAndStatus(alunoId, StatusReserva.PENDENTE);
        return new ReservaResumoResponse((int) ativos, (int) pendentes, emprestimoProps.limitePorAluno());
    }

    @Transactional
    public void cancelar(String emailUsuario, Long reservaId) {
        Long alunoId = resolverAlunoId(emailUsuario);
        Reserva reserva = reservaRepository.findByIdAndAlunoId(reservaId, alunoId)
            .orElseThrow(() -> ResourceNotFoundException.of("Reserva", reservaId));
        if (reserva.getStatus() != StatusReserva.PENDENTE) {
            throw new BusinessException("So e possivel cancelar reservas pendentes.");
        }
        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setDataResolucao(Instant.now(clock));
        reservaRepository.save(reserva);
        liberarExemplar(reserva, "cancelamento");
        log.info("Reserva cancelada id={} livro={} aluno_id={}",
            reservaId, reserva.getLivro().getId(), reserva.getAluno().getId());
    }

    // ---------- Acoes do bibliotecario ----------

    public Page<ReservaResponse> listarPendentes(Pageable pageable) {
        return reservaRepository.findByStatusOrderByDataReservaAsc(StatusReserva.PENDENTE, pageable)
            .map(ReservaResponse::fromMascarado);
    }

    @Transactional
    public ReservaResponse confirmar(Long reservaId, int prazoDias) {
        Reserva reserva = carregarPendente(reservaId);
        // O exemplar ja esta separado pela reserva (situacao RESERVADO).
        Emprestimo emprestimo = emprestimoService.registrarParaReserva(
            reserva.getExemplar(), reserva.getAluno(), prazoDias);
        reserva.setStatus(StatusReserva.CONFIRMADA);
        reserva.setDataResolucao(Instant.now(clock));
        reserva.setEmprestimo(emprestimo);
        log.info("Reserva confirmada id={} exemplar={} aluno_id={} -> emprestimo id={} prazo={}d",
            reservaId, reserva.getExemplar().getId(), reserva.getAluno().getId(),
            emprestimo.getId(), prazoDias);
        Reserva salva = reservaRepository.save(reserva);
        return ReservaResponse.from(salva);
    }

    @Transactional
    public ReservaResponse recusar(Long reservaId) {
        Reserva reserva = carregarPendente(reservaId);
        reserva.setStatus(StatusReserva.RECUSADA);
        reserva.setDataResolucao(Instant.now(clock));
        liberarExemplar(reserva, "recusa");
        log.info("Reserva recusada id={} livro={} aluno_id={}",
            reservaId, reserva.getLivro().getId(), reserva.getAluno().getId());
        Reserva salva = reservaRepository.save(reserva);
        return ReservaResponse.from(salva);
    }

    // ---------- Job de expiracao ----------

    @Transactional
    public int expirarVencidas() {
        List<Reserva> vencidas = reservaRepository
            .findByStatusAndDataExpiracaoBefore(StatusReserva.PENDENTE, LocalDate.now(clock));
        Instant agora = Instant.now(clock);
        for (Reserva reserva : vencidas) {
            reserva.setStatus(StatusReserva.EXPIRADA);
            reserva.setDataResolucao(agora);
            liberarExemplar(reserva, "expiracao");
        }
        reservaRepository.saveAll(vencidas);
        for (Reserva reserva : vencidas) {
            log.info("Reserva expirada id={} livro={} aluno_id={} expirou_em={}",
                reserva.getId(), reserva.getLivro().getId(),
                reserva.getAluno().getId(), reserva.getDataExpiracao());
        }
        return vencidas.size();
    }

    // ---------- Apoio ----------

    /**
     * Libera o exemplar que a reserva segurava de volta pra DISPONIVEL
     * (cancelamento, recusa ou expiracao). Se o exemplar estiver em outro
     * estado (drift), apenas loga sem falhar.
     */
    private void liberarExemplar(Reserva reserva, String motivo) {
        Exemplar exemplar = reserva.getExemplar();
        if (exemplar == null) {
            log.warn("Reserva id={} sem exemplar associado na {} — verificar manualmente",
                reserva.getId(), motivo);
            return;
        }
        if (exemplar.getSituacao() == SituacaoExemplar.RESERVADO) {
            exemplar.setSituacao(SituacaoExemplar.DISPONIVEL);
            exemplarRepository.save(exemplar);
        } else {
            log.warn("Reserva id={} {}: exemplar id={} estava em {} (esperado RESERVADO)",
                reserva.getId(), motivo, exemplar.getId(), exemplar.getSituacao());
        }
    }

    private Long resolverAlunoId(String emailUsuario) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
        Aluno aluno = usuario.getAluno();
        if (aluno == null) {
            throw new BusinessException("Este usuario nao esta vinculado a um aluno.");
        }
        return aluno.getId();
    }

    private Reserva carregarPendente(Long id) {
        Reserva reserva = reservaRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Reserva", id));
        if (reserva.getStatus() != StatusReserva.PENDENTE) {
            throw new BusinessException(
                "Esta reserva nao esta mais pendente (status: " + reserva.getStatus() + ").");
        }
        return reserva;
    }
}
