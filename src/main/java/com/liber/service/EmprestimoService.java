package com.liber.service;

import com.liber.config.EmprestimoProperties;
import com.liber.dto.EditarEmprestimoRequest;
import com.liber.dto.EmprestimoRequest;
import com.liber.dto.EmprestimoResponse;
import com.liber.entity.Aluno;
import com.liber.entity.Emprestimo;
import com.liber.entity.Exemplar;
import com.liber.entity.SituacaoEmprestimo;
import com.liber.entity.SituacaoExemplar;
import com.liber.entity.StatusReserva;
import com.liber.exception.EstoqueIndisponivelException;
import com.liber.exception.RegraEmprestimoException;
import com.liber.exception.ResourceNotFoundException;
import com.liber.repository.AlunoRepository;
import com.liber.repository.EmprestimoRepository;
import com.liber.repository.ExemplarRepository;
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
    private final ExemplarRepository exemplarRepository;
    private final AlunoRepository alunoRepository;
    private final ReservaRepository reservaRepository;
    private final EmprestimoProperties props;
    private final Clock clock;

    public List<EmprestimoResponse> listarAtivos() {
        LocalDate hoje = LocalDate.now(clock);
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

        Aluno aluno = alunoRepository.findByIdForUpdate(req.alunoId())
            .orElseThrow(() -> ResourceNotFoundException.of("Aluno", req.alunoId()));

        LocalDate hoje = LocalDate.now(clock);

        long atrasados = emprestimoRepository.countAtrasadosByAluno(aluno.getId(), hoje);
        if (atrasados > 0) {
            throw new RegraEmprestimoException(
                "Aluno possui %d livro(s) em atraso. Devolva antes de pegar novos emprestimos."
                    .formatted(atrasados));
        }

        // Carrega o exemplar e valida que esta DISPONIVEL. Trocamos a situacao
        // atomicamente — se 2 bibliotecarios tentarem emprestar o mesmo exemplar
        // ao mesmo tempo, @Version no Exemplar dispara OptimisticLockException.
        Exemplar exemplar = exemplarRepository.findById(req.exemplarId())
            .orElseThrow(() -> ResourceNotFoundException.of("Exemplar", req.exemplarId()));
        if (exemplar.getSituacao() != SituacaoExemplar.DISPONIVEL) {
            throw new EstoqueIndisponivelException(exemplar.getLivro().getId());
        }
        exemplar.setSituacao(SituacaoExemplar.EMPRESTADO);
        exemplarRepository.save(exemplar);

        Emprestimo emprestimo = Emprestimo.builder()
            .exemplar(exemplar)
            .aluno(aluno)
            .dataEmprestimo(hoje)
            .prazoDias(req.prazoDias())
            .dataDevolucaoPrevista(hoje.plusDays(req.prazoDias()))
            .situacao(SituacaoEmprestimo.ATIVO)
            .build();

        Emprestimo salvo = emprestimoRepository.save(emprestimo);
        log.info("Emprestimo registrado id={} exemplar={} ({}) aluno_id={} prazo={}d",
            salvo.getId(), exemplar.getId(), exemplar.getCodigo(), aluno.getId(), req.prazoDias());
        return EmprestimoResponse.from(salvo, hoje);
    }

    /**
     * Renova um emprestimo ATIVO, estendendo a data de devolucao prevista.
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

        Long livroId = emp.getExemplar().getLivro().getId();
        long reservasDoLivro = reservaRepository.countByLivroIdAndStatus(livroId, StatusReserva.PENDENTE);
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

    @Transactional
    public void cancelar(Long emprestimoId) {
        Emprestimo emp = emprestimoRepository.findById(emprestimoId)
            .orElseThrow(() -> ResourceNotFoundException.of("Emprestimo", emprestimoId));

        if (emp.getSituacao() != SituacaoEmprestimo.ATIVO) {
            throw new RegraEmprestimoException(
                "So e possivel cancelar emprestimos ativos (situacao atual: " + emp.getSituacao() + ").");
        }

        Exemplar exemplar = emp.getExemplar();
        exemplar.setSituacao(SituacaoExemplar.DISPONIVEL);
        exemplarRepository.save(exemplar);

        emp.setSituacao(SituacaoEmprestimo.CANCELADO);
        emprestimoRepository.save(emp);
        log.info("Emprestimo cancelado id={} exemplar={} ({}) aluno_id={} (lancamento incorreto)",
            emp.getId(), exemplar.getId(), exemplar.getCodigo(), emp.getAluno().getId());
    }

    @Transactional
    public EmprestimoResponse registrarDevolucao(Long emprestimoId) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
            .orElseThrow(() -> ResourceNotFoundException.of("Emprestimo", emprestimoId));

        if (emprestimo.getSituacao() == SituacaoEmprestimo.DEVOLVIDO) {
            throw new RegraEmprestimoException(
                "Emprestimo ja devolvido em " + emprestimo.getDataDevolucaoEfetiva());
        }

        Exemplar exemplar = emprestimo.getExemplar();
        if (exemplar.getSituacao() == SituacaoExemplar.EMPRESTADO) {
            exemplar.setSituacao(SituacaoExemplar.DISPONIVEL);
            exemplarRepository.save(exemplar);
        } else {
            log.warn("Devolucao do emprestimo id={} mas exemplar id={} estava em {} (esperado EMPRESTADO)",
                emprestimoId, exemplar.getId(), exemplar.getSituacao());
        }

        LocalDate hoje = LocalDate.now(clock);
        emprestimo.setSituacao(SituacaoEmprestimo.DEVOLVIDO);
        emprestimo.setDataDevolucaoEfetiva(hoje);

        Emprestimo salvo = emprestimoRepository.save(emprestimo);
        log.info("Devolucao registrada emprestimo id={} exemplar={} ({}) aluno_id={}",
            salvo.getId(), exemplar.getId(), exemplar.getCodigo(), emprestimo.getAluno().getId());
        return EmprestimoResponse.from(salvo, hoje);
    }

    /**
     * Cria um emprestimo a partir de uma reserva confirmada. O exemplar ja foi
     * separado pela reserva (situacao RESERVADO). NAO chamar fora de
     * {@code ReservaService.confirmar}.
     */
    @Transactional
    Emprestimo registrarParaReserva(Exemplar exemplar, Aluno aluno, int prazoDias) {
        validarPrazo(prazoDias);
        LocalDate hoje = LocalDate.now(clock);
        exemplar.setSituacao(SituacaoExemplar.EMPRESTADO);
        exemplarRepository.save(exemplar);
        Emprestimo emprestimo = Emprestimo.builder()
            .exemplar(exemplar)
            .aluno(aluno)
            .dataEmprestimo(hoje)
            .prazoDias(prazoDias)
            .dataDevolucaoPrevista(hoje.plusDays(prazoDias))
            .situacao(SituacaoEmprestimo.ATIVO)
            .build();
        Emprestimo salvo = emprestimoRepository.save(emprestimo);
        log.info("Emprestimo criado a partir de reserva id={} exemplar={} aluno_id={}",
            salvo.getId(), exemplar.getId(), aluno.getId());
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
