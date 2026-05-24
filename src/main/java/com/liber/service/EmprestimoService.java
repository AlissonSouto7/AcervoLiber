package com.liber.service;

import com.liber.config.EmprestimoProperties;
import com.liber.dto.EditarEmprestimoRequest;
import com.liber.dto.EmprestimoRequest;
import com.liber.dto.EmprestimoResponse;
import com.liber.entity.Aluno;
import com.liber.entity.Emprestimo;
import com.liber.entity.Livro;
import com.liber.entity.SituacaoEmprestimo;
import com.liber.entity.StatusReserva;
import com.liber.exception.EstoqueIndisponivelException;
import com.liber.exception.RegraEmprestimoException;
import com.liber.exception.ResourceNotFoundException;
import com.liber.repository.AlunoRepository;
import com.liber.repository.EmprestimoRepository;
import com.liber.repository.LivroRepository;
import com.liber.repository.ReservaRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
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
public class EmprestimoService {

    private final EmprestimoRepository emprestimoRepository;
    private final LivroRepository livroRepository;
    private final AlunoRepository alunoRepository;
    private final ReservaRepository reservaRepository;
    private final EmprestimoProperties props;
    private final Clock clock;

    public List<EmprestimoResponse> listarAtivos() {
        LocalDate hoje = LocalDate.now(clock);
        // Tela visivel a quem passar atras do balcao (visitante, pais de outros alunos)
        // — matricula mascarada por LGPD §14 (dados de menores).
        return emprestimoRepository
            .findBySituacaoOrderByDataDevolucaoPrevistaAsc(SituacaoEmprestimo.ATIVO)
            .stream()
            .map(e -> EmprestimoResponse.fromMascarado(e, hoje))
            .toList();
    }

    public Page<EmprestimoResponse> listarHistorico(Pageable pageable) {
        LocalDate hoje = LocalDate.now(clock);
        return emprestimoRepository.findAllByOrderByDataEmprestimoDesc(pageable)
            .map(e -> EmprestimoResponse.from(e, hoje));
    }

    public Page<EmprestimoResponse> listarPorAluno(Long alunoId, Pageable pageable) {
        if (!alunoRepository.existsById(alunoId)) {
            throw ResourceNotFoundException.of("Aluno", alunoId);
        }
        LocalDate hoje = LocalDate.now(clock);
        return emprestimoRepository.findByAlunoIdOrderByDataEmprestimoDesc(alunoId, pageable)
            .map(e -> EmprestimoResponse.from(e, hoje));
    }

    public EmprestimoResponse buscarPorId(Long id) {
        Emprestimo emp = emprestimoRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Emprestimo", id));
        return EmprestimoResponse.from(emp, LocalDate.now(clock));
    }

    @Transactional
    public EmprestimoResponse registrar(EmprestimoRequest req) {
        validarPrazo(req.prazoDias());

        // SELECT FOR UPDATE no aluno — serializa requests concorrentes para o mesmo
        // aluno, fechando a janela de corrida entre o count e o save.
        Aluno aluno = alunoRepository.findByIdForUpdate(req.alunoId())
            .orElseThrow(() -> ResourceNotFoundException.of("Aluno", req.alunoId()));

        LocalDate hoje = LocalDate.now(clock);

        // Bloqueio por atraso: aluno com livro vencido nao pega novo no balcao.
        // Regra de biblioteca escolar — sem isso o aluno acumula atrasos indefinidamente.
        long atrasados = emprestimoRepository.countAtrasadosByAluno(aluno.getId(), hoje);
        if (atrasados > 0) {
            throw new RegraEmprestimoException(
                "Aluno possui %d livro(s) em atraso. Devolva antes de pegar novos emprestimos."
                    .formatted(atrasados));
        }

        // O limite por aluno NAO bloqueia emprestimo manual feito pelo staff —
        // este endpoint so e chamado por bibliotecario/admin (aluno usa /reservas
        // pra demandar, e la o limite e enforced em ReservaService). A direcao
        // pode liberar manualmente livro extra pra aluno de confianca / projeto
        // especial, sem precisar mexer na config global.

        int atualizadas = livroRepository.decrementarEstoque(req.livroId());
        if (atualizadas == 0) {
            if (!livroRepository.existsById(req.livroId())) {
                throw ResourceNotFoundException.of("Livro", req.livroId());
            }
            throw new EstoqueIndisponivelException(req.livroId());
        }

        Livro livro = livroRepository.findById(req.livroId())
            .orElseThrow(() -> ResourceNotFoundException.of("Livro", req.livroId()));

        Emprestimo emprestimo = Emprestimo.builder()
            .livro(livro)
            .aluno(aluno)
            .dataEmprestimo(hoje)
            .prazoDias(req.prazoDias())
            .dataDevolucaoPrevista(hoje.plusDays(req.prazoDias()))
            .situacao(SituacaoEmprestimo.ATIVO)
            .build();

        Emprestimo salvo = emprestimoRepository.save(emprestimo);
        log.info("Emprestimo registrado id={} livro={} aluno_matricula={} prazo={}d",
            salvo.getId(), livro.getId(), aluno.getMatricula(), req.prazoDias());
        return EmprestimoResponse.from(salvo, hoje);
    }

    /**
     * Renova um empréstimo ATIVO, estendendo a data de devolucao prevista a partir
     * de hoje + {@code prazoDias}. Incrementa o contador de renovacoes da entidade.
     *
     * <p>Bloqueios:
     * <ul>
     *   <li>Empréstimo nao-ATIVO (ja devolvido) — 422 "ja foi devolvido"</li>
     *   <li>Empréstimo em atraso — 422 "devolva antes de renovar"</li>
     *   <li>Limite de renovacoes atingido — 422 com contador</li>
     *   <li>Existe reserva PENDENTE do mesmo livro por outro aluno — 422 "outro aluno aguardando"</li>
     *   <li>Prazo solicitado acima do maximo — 422 padrao</li>
     * </ul>
     */
    @Transactional
    public EmprestimoResponse renovar(Long emprestimoId, int prazoDias) {
        validarPrazo(prazoDias);

        Emprestimo emp = emprestimoRepository.findById(emprestimoId)
            .orElseThrow(() -> ResourceNotFoundException.of("Emprestimo", emprestimoId));

        if (emp.getSituacao() != SituacaoEmprestimo.ATIVO) {
            throw new RegraEmprestimoException(
                "Emprestimo nao esta ativo (situacao: " + emp.getSituacao() + ")");
        }

        LocalDate hoje = LocalDate.now(clock);
        if (emp.getDataDevolucaoPrevista().isBefore(hoje)) {
            throw new RegraEmprestimoException(
                "Emprestimo esta em atraso. Devolva o livro antes de renovar.");
        }

        if (emp.getRenovacoes() >= props.maxRenovacoes()) {
            throw new RegraEmprestimoException(
                "Limite de %d renovacoes atingido para este emprestimo."
                    .formatted(props.maxRenovacoes()));
        }

        // Bloqueia renovacao se outro aluno esta aguardando o mesmo livro — padrao
        // de biblioteca: quem reservou tem prioridade sobre quem ja esta com o livro.
        long reservasDoLivro = reservaRepository.countByLivroIdAndStatus(
            emp.getLivro().getId(), StatusReserva.PENDENTE);
        if (reservasDoLivro > 0) {
            throw new RegraEmprestimoException(
                "Existe(m) %d reserva(s) pendente(s) para este livro. Renovacao bloqueada."
                    .formatted(reservasDoLivro));
        }

        emp.setDataDevolucaoPrevista(hoje.plusDays(prazoDias));
        emp.setPrazoDias(prazoDias);
        emp.setRenovacoes(emp.getRenovacoes() + 1);

        Emprestimo salvo = emprestimoRepository.save(emp);
        log.info("Renovacao registrada emprestimo id={} renovacao#{} novoVencimento={}",
            salvo.getId(), salvo.getRenovacoes(), salvo.getDataDevolucaoPrevista());
        return EmprestimoResponse.from(salvo, hoje);
    }

    /**
     * Edita campos de um empréstimo ATIVO para corrigir lancamento errado.
     * Aceita data de empréstimo e/ou prazo (apenas os enviados sao alterados).
     * Recalcula automaticamente a data de devolucao prevista.
     *
     * <p>Bloqueios: empréstimo nao-ATIVO; payload vazio; data de empréstimo no
     * futuro; vencimento resultante no passado.
     */
    @Transactional
    public EmprestimoResponse editar(Long emprestimoId, EditarEmprestimoRequest req) {
        if (req.isVazio()) {
            throw new RegraEmprestimoException(
                "Informe ao menos um campo para editar (dataEmprestimo ou prazoDias).");
        }

        Emprestimo emp = emprestimoRepository.findById(emprestimoId)
            .orElseThrow(() -> ResourceNotFoundException.of("Emprestimo", emprestimoId));

        if (emp.getSituacao() != SituacaoEmprestimo.ATIVO) {
            throw new RegraEmprestimoException(
                "So e possivel editar emprestimos ativos (situacao atual: " + emp.getSituacao() + ").");
        }

        LocalDate hoje = LocalDate.now(clock);
        LocalDate antesData = emp.getDataEmprestimo();
        Integer antesPrazo = emp.getPrazoDias();

        if (req.dataEmprestimo() != null) {
            if (req.dataEmprestimo().isAfter(hoje)) {
                throw new RegraEmprestimoException("Data de emprestimo nao pode ser no futuro.");
            }
            emp.setDataEmprestimo(req.dataEmprestimo());
        }
        if (req.prazoDias() != null) {
            validarPrazo(req.prazoDias());
            emp.setPrazoDias(req.prazoDias());
        }

        LocalDate novoVencimento = emp.getDataEmprestimo().plusDays(emp.getPrazoDias());
        // Coerencia: nao deixa o BIB salvar com vencimento no passado (criaria
        // emprestimo ja atrasado pela edicao, confundindo as regras de bloqueio).
        if (novoVencimento.isBefore(hoje)) {
            throw new RegraEmprestimoException(
                "Data de devolucao resultante (%s) esta no passado.".formatted(novoVencimento));
        }
        emp.setDataDevolucaoPrevista(novoVencimento);

        Emprestimo salvo = emprestimoRepository.save(emp);
        log.info("Emprestimo editado id={} (data {}->{}, prazo {}->{})",
            salvo.getId(), antesData, salvo.getDataEmprestimo(), antesPrazo, salvo.getPrazoDias());
        return EmprestimoResponse.from(salvo, hoje);
    }

    /**
     * Cancela um empréstimo (lancamento errado pelo bibliotecario). Marca como
     * CANCELADO (soft delete, preserva FK de reservas confirmadas) e devolve
     * o livro ao estoque. Diferente de devolucao — esse caminho indica que o
     * empréstimo nunca devia ter existido.
     *
     * <p>Bloqueio: nao cancela empréstimo nao-ATIVO (DEVOLVIDO/CANCELADO).
     */
    @Transactional
    public void cancelar(Long emprestimoId) {
        Emprestimo emp = emprestimoRepository.findById(emprestimoId)
            .orElseThrow(() -> ResourceNotFoundException.of("Emprestimo", emprestimoId));

        if (emp.getSituacao() != SituacaoEmprestimo.ATIVO) {
            throw new RegraEmprestimoException(
                "So e possivel cancelar emprestimos ativos (situacao atual: " + emp.getSituacao() + ").");
        }

        int atualizadas = livroRepository.incrementarEstoque(emp.getLivro().getId());
        if (atualizadas == 0) {
            // Drift de estoque: nao acumular silenciosamente — log nivel WARN
            // garante visibilidade nos logs JSON da aplicacao.
            log.warn("ESTOQUE_DIVERGENCIA: cancelamento do emprestimo id={} nao incrementou estoque do livro id={} (provavelmente ja no teto)",
                emprestimoId, emp.getLivro().getId());
        }

        emp.setSituacao(SituacaoEmprestimo.CANCELADO);
        emprestimoRepository.save(emp);
        log.info("Emprestimo cancelado id={} livro={} aluno_matricula={} (lancamento incorreto)",
            emp.getId(), emp.getLivro().getId(), emp.getAluno().getMatricula());
    }

    @Transactional
    public EmprestimoResponse registrarDevolucao(Long emprestimoId) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
            .orElseThrow(() -> ResourceNotFoundException.of("Emprestimo", emprestimoId));

        if (emprestimo.getSituacao() == SituacaoEmprestimo.DEVOLVIDO) {
            throw new RegraEmprestimoException(
                "Emprestimo ja devolvido em " + emprestimo.getDataDevolucaoEfetiva());
        }

        int atualizadas = livroRepository.incrementarEstoque(emprestimo.getLivro().getId());
        if (atualizadas == 0) {
            log.warn("ESTOQUE_DIVERGENCIA: devolucao do emprestimo id={} nao incrementou estoque do livro id={} (provavelmente ja no teto)",
                emprestimoId, emprestimo.getLivro().getId());
        }

        LocalDate hoje = LocalDate.now(clock);
        emprestimo.setSituacao(SituacaoEmprestimo.DEVOLVIDO);
        emprestimo.setDataDevolucaoEfetiva(hoje);

        Emprestimo salvo = emprestimoRepository.save(emprestimo);
        log.info("Devolucao registrada emprestimo id={} livro={} aluno_matricula={}",
            salvo.getId(), emprestimo.getLivro().getId(), emprestimo.getAluno().getMatricula());
        return EmprestimoResponse.from(salvo, hoje);
    }

    /**
     * Cria um emprestimo a partir de uma reserva confirmada. NAO decrementa o
     * estoque — a reserva ja segurou o exemplar quando foi criada. NAO chamar
     * fora de {@code ReservaService.confirmar} (visibilidade package-private —
     * sem reserva valida, criaria empréstimo fantasma).
     * <p>DEVE rodar dentro da transacao do chamador (propagation REQUIRED).
     */
    @Transactional
    Emprestimo registrarParaReserva(Livro livro, Aluno aluno, int prazoDias) {
        validarPrazo(prazoDias);
        LocalDate hoje = LocalDate.now(clock);
        Emprestimo emprestimo = Emprestimo.builder()
            .livro(livro)
            .aluno(aluno)
            .dataEmprestimo(hoje)
            .prazoDias(prazoDias)
            .dataDevolucaoPrevista(hoje.plusDays(prazoDias))
            .situacao(SituacaoEmprestimo.ATIVO)
            .build();
        Emprestimo salvo = emprestimoRepository.save(emprestimo);
        log.info("Emprestimo criado a partir de reserva id={} livro={} aluno={}",
            salvo.getId(), livro.getId(), aluno.getId());
        return salvo;
    }

    private void validarPrazo(int prazoDias) {
        if (prazoDias > props.prazoMaximoDias()) {
            throw new RegraEmprestimoException(
                "Prazo solicitado (%d dias) excede o maximo permitido (%d dias)"
                    .formatted(prazoDias, props.prazoMaximoDias()));
        }
    }
}
