package com.liber.service;

import com.liber.config.EmprestimoProperties;
import com.liber.config.ReservaProperties;
import com.liber.dto.ReservaResponse;
import com.liber.dto.ReservaResumoResponse;
import com.liber.entity.Aluno;
import com.liber.entity.Emprestimo;
import com.liber.entity.Livro;
import com.liber.entity.Reserva;
import com.liber.entity.SituacaoEmprestimo;
import com.liber.entity.StatusReserva;
import com.liber.entity.Usuario;
import com.liber.exception.BusinessException;
import com.liber.exception.EstoqueIndisponivelException;
import com.liber.exception.ResourceNotFoundException;
import com.liber.repository.AlunoRepository;
import com.liber.repository.EmprestimoRepository;
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
 * <p>Fluxo: o aluno reserva um livro disponivel (a reserva PENDENTE segura um
 * exemplar via decremento atomico do estoque). O bibliotecario confirma —
 * gerando um emprestimo sem novo decremento — ou recusa. O aluno pode cancelar
 * enquanto pendente. Reservas nao retiradas expiram pelo job de expiracao.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final LivroRepository livroRepository;
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

        // SELECT FOR UPDATE no aluno — serializa reservas concorrentes do mesmo aluno
        Aluno aluno = alunoRepository.findByIdForUpdate(alunoId)
            .orElseThrow(() -> ResourceNotFoundException.of("Aluno", alunoId));

        if (reservaRepository.existsByAlunoIdAndLivroIdAndStatus(alunoId, livroId, StatusReserva.PENDENTE)) {
            throw new BusinessException("Voce ja tem uma reserva pendente para este livro.");
        }

        LocalDate hoje = LocalDate.now(clock);

        // Mesmo bloqueio do emprestimo direto: aluno em atraso nao pode nem reservar.
        // Sem isso, ele contornaria o bloqueio do balcao indo pelo portal de reservas.
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

        // Segura um exemplar (decremento atomico, igual ao emprestimo)
        int atualizadas = livroRepository.decrementarEstoque(livroId);
        if (atualizadas == 0) {
            if (!livroRepository.existsById(livroId)) {
                throw ResourceNotFoundException.of("Livro", livroId);
            }
            throw new EstoqueIndisponivelException(livroId);
        }
        Livro livro = livroRepository.findById(livroId)
            .orElseThrow(() -> ResourceNotFoundException.of("Livro", livroId));

        Reserva reserva = Reserva.builder()
            .livro(livro)
            .aluno(aluno)
            .status(StatusReserva.PENDENTE)
            .dataReserva(hoje)
            .dataExpiracao(hoje.plusDays(reservaProps.validadeDias()))
            .build();
        Reserva salva = reservaRepository.save(reserva);
        log.info("Reserva criada id={} livro={} aluno_matricula={} validade={}",
            salva.getId(), livroId, aluno.getMatricula(), salva.getDataExpiracao());
        return ReservaResponse.from(salva);
    }

    public Page<ReservaResponse> listarMinhas(String emailUsuario, Pageable pageable) {
        Long alunoId = resolverAlunoId(emailUsuario);
        return reservaRepository.findByAlunoIdOrderByDataReservaDesc(alunoId, pageable)
            .map(ReservaResponse::from);
    }

    /** Resumo de emprestimos/reservas do aluno e o limite — alimenta o catalogo. */
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
        devolverExemplar(reserva, "cancelamento");
        log.info("Reserva cancelada id={} livro={} aluno_matricula={}",
            reservaId, reserva.getLivro().getId(), reserva.getAluno().getMatricula());
    }

    // ---------- Acoes do bibliotecario ----------

    public Page<ReservaResponse> listarPendentes(Pageable pageable) {
        // Fila do bibliotecario visivel a quem passar atras do balcao — matricula
        // mascarada por LGPD §14. Nome+turma continuam para identificacao.
        return reservaRepository.findByStatusOrderByDataReservaAsc(StatusReserva.PENDENTE, pageable)
            .map(ReservaResponse::fromMascarado);
    }

    @Transactional
    public ReservaResponse confirmar(Long reservaId, int prazoDias) {
        Reserva reserva = carregarPendente(reservaId);
        // O exemplar ja foi segurado pela reserva — o emprestimo NAO decrementa de novo
        Emprestimo emprestimo = emprestimoService.registrarParaReserva(
            reserva.getLivro(), reserva.getAluno(), prazoDias);
        reserva.setStatus(StatusReserva.CONFIRMADA);
        reserva.setDataResolucao(Instant.now(clock));
        reserva.setEmprestimo(emprestimo);
        log.info("Reserva confirmada id={} livro={} aluno_matricula={} -> emprestimo id={} prazo={}d",
            reservaId, reserva.getLivro().getId(), reserva.getAluno().getMatricula(),
            emprestimo.getId(), prazoDias);
        Reserva salva = reservaRepository.save(reserva);
        return ReservaResponse.from(salva);
    }

    @Transactional
    public ReservaResponse recusar(Long reservaId) {
        Reserva reserva = carregarPendente(reservaId);
        reserva.setStatus(StatusReserva.RECUSADA);
        reserva.setDataResolucao(Instant.now(clock));
        devolverExemplar(reserva, "recusa");
        log.info("Reserva recusada id={} livro={} aluno_matricula={}",
            reservaId, reserva.getLivro().getId(), reserva.getAluno().getMatricula());
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
            devolverExemplar(reserva, "expiracao");
        }
        reservaRepository.saveAll(vencidas);
        for (Reserva reserva : vencidas) {
            log.info("Reserva expirada id={} livro={} aluno_matricula={} expirou_em={}",
                reserva.getId(), reserva.getLivro().getId(),
                reserva.getAluno().getMatricula(), reserva.getDataExpiracao());
        }
        return vencidas.size();
    }

    // ---------- Apoio ----------

    /**
     * Devolve ao estoque o exemplar que a reserva segurava (cancelamento, recusa
     * ou expiracao). Se o incremento nao afetar linhas, registra um alerta — isso
     * indica divergencia de estoque que precisa de correcao manual.
     */
    private void devolverExemplar(Reserva reserva, String motivo) {
        int devolvidas = livroRepository.incrementarEstoque(reserva.getLivro().getId());
        if (devolvidas == 0) {
            log.warn("Estoque NAO devolvido na {} da reserva id={} (livro id={}): "
                    + "incremento nao afetou linhas — divergencia de estoque, verificar manualmente",
                motivo, reserva.getId(), reserva.getLivro().getId());
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
