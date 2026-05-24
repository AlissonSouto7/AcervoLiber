package com.liber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.liber.config.EmprestimoProperties;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmprestimoServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 5, 10);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-10T12:00:00Z"), ZoneOffset.UTC);

    @Mock EmprestimoRepository emprestimoRepository;
    @Mock LivroRepository livroRepository;
    @Mock AlunoRepository alunoRepository;
    @Mock ReservaRepository reservaRepository;
    @Mock AuditService auditService;

    private final EmprestimoProperties props = new EmprestimoProperties(7, 30, 3, 2);
    private EmprestimoService service;

    @BeforeEach
    void setUp() {
        service = new EmprestimoService(
            emprestimoRepository, livroRepository, alunoRepository, reservaRepository,
            auditService, props, CLOCK);
    }

    private static Aluno aluno() {
        return Aluno.builder().id(10L).matricula("2026A").nome("Joao").turma("9A").build();
    }

    private static Livro livro() {
        return Livro.builder()
            .id(20L).titulo("Livro").autor("Autor")
            .quantidadeExemplares(3).quantidadeDisponivel(2).build();
    }

    @Test
    void registrar_happy_path_cria_emprestimo_e_decrementa_estoque() {
        EmprestimoRequest req = new EmprestimoRequest(20L, 10L, 7);
        when(alunoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(aluno()));
        when(emprestimoRepository.countByAlunoIdAndSituacao(10L, SituacaoEmprestimo.ATIVO)).thenReturn(0L);
        when(livroRepository.decrementarEstoque(20L)).thenReturn(1);
        when(livroRepository.findById(20L)).thenReturn(Optional.of(livro()));
        when(emprestimoRepository.save(any(Emprestimo.class))).thenAnswer(inv -> {
            Emprestimo e = inv.getArgument(0);
            e.setId(100L);
            return e;
        });

        EmprestimoResponse resp = service.registrar(req);

        assertThat(resp.id()).isEqualTo(100L);
        assertThat(resp.dataEmprestimo()).isEqualTo(HOJE);
        assertThat(resp.dataDevolucaoPrevista()).isEqualTo(HOJE.plusDays(7));
        assertThat(resp.situacao()).isEqualTo(SituacaoEmprestimo.ATIVO);
        verify(livroRepository).decrementarEstoque(20L);
    }

    @Test
    void registrar_rejeita_prazo_acima_do_maximo() {
        EmprestimoRequest req = new EmprestimoRequest(20L, 10L, 31); // max=30

        assertThatThrownBy(() -> service.registrar(req))
            .isInstanceOf(RegraEmprestimoException.class)
            .hasMessageContaining("maximo");

        verify(livroRepository, never()).decrementarEstoque(any());
    }

    @Test
    void registrar_rejeita_quando_aluno_ja_atingiu_o_limite() {
        EmprestimoRequest req = new EmprestimoRequest(20L, 10L, 7);
        when(alunoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(aluno()));
        when(emprestimoRepository.countByAlunoIdAndSituacao(10L, SituacaoEmprestimo.ATIVO))
            .thenReturn(3L); // limit = 3

        assertThatThrownBy(() -> service.registrar(req))
            .isInstanceOf(RegraEmprestimoException.class)
            .hasMessageContaining("limite");

        verify(livroRepository, never()).decrementarEstoque(any());
    }

    @Test
    void registrar_rejeita_quando_aluno_tem_livro_em_atraso() {
        EmprestimoRequest req = new EmprestimoRequest(20L, 10L, 7);
        when(alunoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(aluno()));
        when(emprestimoRepository.countAtrasadosByAluno(10L, HOJE)).thenReturn(2L);

        assertThatThrownBy(() -> service.registrar(req))
            .isInstanceOf(RegraEmprestimoException.class)
            .hasMessageContaining("atraso");

        verify(livroRepository, never()).decrementarEstoque(any());
    }

    @Test
    void registrar_lanca_NotFound_se_aluno_nao_existe() {
        EmprestimoRequest req = new EmprestimoRequest(20L, 99L, 7);
        when(alunoRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(req))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registrar_lanca_EstoqueIndisponivel_quando_decremento_falha_e_livro_existe() {
        EmprestimoRequest req = new EmprestimoRequest(20L, 10L, 7);
        when(alunoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(aluno()));
        when(emprestimoRepository.countByAlunoIdAndSituacao(10L, SituacaoEmprestimo.ATIVO)).thenReturn(0L);
        when(livroRepository.decrementarEstoque(20L)).thenReturn(0);
        when(livroRepository.existsById(20L)).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(req))
            .isInstanceOf(EstoqueIndisponivelException.class);
    }

    @Test
    void registrar_lanca_NotFound_quando_decremento_falha_e_livro_nao_existe() {
        EmprestimoRequest req = new EmprestimoRequest(99L, 10L, 7);
        when(alunoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(aluno()));
        when(emprestimoRepository.countByAlunoIdAndSituacao(10L, SituacaoEmprestimo.ATIVO)).thenReturn(0L);
        when(livroRepository.decrementarEstoque(99L)).thenReturn(0);
        when(livroRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.registrar(req))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registrarDevolucao_happy_path_marca_DEVOLVIDO_e_incrementa_estoque() {
        Emprestimo emp = Emprestimo.builder()
            .id(100L).livro(livro()).aluno(aluno())
            .dataEmprestimo(HOJE.minusDays(3)).prazoDias(7)
            .dataDevolucaoPrevista(HOJE.plusDays(4))
            .situacao(SituacaoEmprestimo.ATIVO)
            .build();
        when(emprestimoRepository.findById(100L)).thenReturn(Optional.of(emp));
        when(livroRepository.incrementarEstoque(20L)).thenReturn(1);
        when(emprestimoRepository.save(any(Emprestimo.class))).thenAnswer(inv -> inv.getArgument(0));

        EmprestimoResponse resp = service.registrarDevolucao(100L);

        assertThat(resp.situacao()).isEqualTo(SituacaoEmprestimo.DEVOLVIDO);
        assertThat(resp.dataDevolucaoEfetiva()).isEqualTo(HOJE);
        verify(livroRepository).incrementarEstoque(20L);
    }

    @Test
    void registrarDevolucao_rejeita_emprestimo_ja_devolvido() {
        Emprestimo emp = Emprestimo.builder()
            .id(100L).livro(livro()).aluno(aluno())
            .dataEmprestimo(HOJE.minusDays(10)).prazoDias(7)
            .dataDevolucaoPrevista(HOJE.minusDays(3))
            .dataDevolucaoEfetiva(HOJE.minusDays(2))
            .situacao(SituacaoEmprestimo.DEVOLVIDO)
            .build();
        when(emprestimoRepository.findById(100L)).thenReturn(Optional.of(emp));

        assertThatThrownBy(() -> service.registrarDevolucao(100L))
            .isInstanceOf(RegraEmprestimoException.class)
            .hasMessageContaining("ja devolvido");

        verify(livroRepository, never()).incrementarEstoque(eq(20L));
    }

    // ---------------------- Renovacao ----------------------

    private static Emprestimo emprestimoAtivo(int diasParaVencer, int renovacoesJaFeitas) {
        return Emprestimo.builder()
            .id(100L).livro(livro()).aluno(aluno())
            .dataEmprestimo(HOJE.minusDays(5)).prazoDias(7)
            .dataDevolucaoPrevista(HOJE.plusDays(diasParaVencer))
            .situacao(SituacaoEmprestimo.ATIVO)
            .renovacoes(renovacoesJaFeitas)
            .build();
    }

    @Test
    void renovar_happy_path_estende_prazo_e_incrementa_contador() {
        Emprestimo emp = emprestimoAtivo(2, 0);
        when(emprestimoRepository.findById(100L)).thenReturn(Optional.of(emp));
        when(reservaRepository.countByLivroIdAndStatus(20L, StatusReserva.PENDENTE)).thenReturn(0L);
        when(emprestimoRepository.save(any(Emprestimo.class))).thenAnswer(inv -> inv.getArgument(0));

        EmprestimoResponse resp = service.renovar(100L, 7);

        assertThat(resp.dataDevolucaoPrevista()).isEqualTo(HOJE.plusDays(7));
        assertThat(emp.getRenovacoes()).isEqualTo(1);
    }

    @Test
    void renovar_rejeita_emprestimo_nao_ativo() {
        Emprestimo emp = emprestimoAtivo(2, 0);
        emp.setSituacao(SituacaoEmprestimo.DEVOLVIDO);
        when(emprestimoRepository.findById(100L)).thenReturn(Optional.of(emp));

        assertThatThrownBy(() -> service.renovar(100L, 7))
            .isInstanceOf(RegraEmprestimoException.class)
            .hasMessageContaining("nao esta ativo");
    }

    @Test
    void renovar_rejeita_emprestimo_em_atraso() {
        Emprestimo emp = emprestimoAtivo(-1, 0); // venceu ontem
        when(emprestimoRepository.findById(100L)).thenReturn(Optional.of(emp));

        assertThatThrownBy(() -> service.renovar(100L, 7))
            .isInstanceOf(RegraEmprestimoException.class)
            .hasMessageContaining("atraso");
    }

    @Test
    void renovar_rejeita_quando_limite_atingido() {
        Emprestimo emp = emprestimoAtivo(3, 2); // limite=2, ja renovou 2x
        when(emprestimoRepository.findById(100L)).thenReturn(Optional.of(emp));

        assertThatThrownBy(() -> service.renovar(100L, 7))
            .isInstanceOf(RegraEmprestimoException.class)
            .hasMessageContaining("Limite");
    }

    @Test
    void renovar_rejeita_quando_outro_aluno_tem_reserva_pendente() {
        Emprestimo emp = emprestimoAtivo(3, 0);
        when(emprestimoRepository.findById(100L)).thenReturn(Optional.of(emp));
        when(reservaRepository.countByLivroIdAndStatus(20L, StatusReserva.PENDENTE)).thenReturn(1L);

        assertThatThrownBy(() -> service.renovar(100L, 7))
            .isInstanceOf(RegraEmprestimoException.class)
            .hasMessageContaining("reserva");
    }
}
