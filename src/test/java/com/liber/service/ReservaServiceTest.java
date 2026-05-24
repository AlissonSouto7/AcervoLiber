package com.liber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.liber.config.EmprestimoProperties;
import com.liber.config.ReservaProperties;
import com.liber.dto.ReservaResponse;
import com.liber.entity.Aluno;
import com.liber.entity.Emprestimo;
import com.liber.entity.Livro;
import com.liber.entity.Reserva;
import com.liber.entity.Role;
import com.liber.entity.SituacaoEmprestimo;
import com.liber.entity.StatusReserva;
import com.liber.entity.Usuario;
import com.liber.exception.BusinessException;
import com.liber.exception.EstoqueIndisponivelException;
import com.liber.repository.AlunoRepository;
import com.liber.repository.EmprestimoRepository;
import com.liber.repository.LivroRepository;
import com.liber.repository.ReservaRepository;
import com.liber.repository.UsuarioRepository;
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
class ReservaServiceTest {

    private static final String EMAIL = "aluno.2026001@liber.local";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-22T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate HOJE = LocalDate.of(2026, 5, 22);

    @Mock ReservaRepository reservaRepository;
    @Mock LivroRepository livroRepository;
    @Mock AlunoRepository alunoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock EmprestimoRepository emprestimoRepository;
    @Mock EmprestimoService emprestimoService;
    @Mock AuditService auditService;

    private final ReservaProperties reservaProps = new ReservaProperties(3);
    private final EmprestimoProperties emprestimoProps = new EmprestimoProperties(7, 30, 3, 2);
    private ReservaService service;

    @BeforeEach
    void setUp() {
        service = new ReservaService(reservaRepository, livroRepository, alunoRepository,
            usuarioRepository, emprestimoRepository, emprestimoService, auditService,
            reservaProps, emprestimoProps, CLOCK);
    }

    private static Aluno aluno() {
        return Aluno.builder().id(10L).matricula("2026001").nome("Ana Beatriz").turma("9A").build();
    }

    private static Usuario usuarioAluno(Aluno aluno) {
        return Usuario.builder()
            .id(50L).email(EMAIL).nome("Ana Beatriz").senhaHash("h")
            .role(Role.ALUNO).ativo(true).passwordChangedAt(Instant.now(CLOCK)).aluno(aluno)
            .build();
    }

    private static Livro livro() {
        return Livro.builder()
            .id(20L).titulo("Dom Casmurro").autor("Machado de Assis")
            .quantidadeExemplares(3).quantidadeDisponivel(2).build();
    }

    private static Reserva reservaPendente() {
        return Reserva.builder()
            .id(99L).livro(livro()).aluno(aluno())
            .status(StatusReserva.PENDENTE).dataReserva(HOJE).dataExpiracao(HOJE.plusDays(3))
            .build();
    }

    @Test
    void reservar_happy_path_segura_exemplar_e_cria_reserva_pendente() {
        Aluno aluno = aluno();
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuarioAluno(aluno)));
        when(alunoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(aluno));
        when(reservaRepository.existsByAlunoIdAndLivroIdAndStatus(10L, 20L, StatusReserva.PENDENTE))
            .thenReturn(false);
        when(emprestimoRepository.countByAlunoIdAndSituacao(10L, SituacaoEmprestimo.ATIVO)).thenReturn(0L);
        when(reservaRepository.countByAlunoIdAndStatus(10L, StatusReserva.PENDENTE)).thenReturn(0L);
        when(livroRepository.decrementarEstoque(20L)).thenReturn(1);
        when(livroRepository.findById(20L)).thenReturn(Optional.of(livro()));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            r.setId(99L);
            return r;
        });

        ReservaResponse resp = service.reservar(EMAIL, 20L);

        assertThat(resp.status()).isEqualTo(StatusReserva.PENDENTE);
        assertThat(resp.dataReserva()).isEqualTo(HOJE);
        assertThat(resp.dataExpiracao()).isEqualTo(HOJE.plusDays(3));
        verify(livroRepository).decrementarEstoque(20L);
    }

    @Test
    void reservar_rejeita_reserva_duplicada_do_mesmo_livro() {
        Aluno aluno = aluno();
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuarioAluno(aluno)));
        when(alunoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(aluno));
        when(reservaRepository.existsByAlunoIdAndLivroIdAndStatus(10L, 20L, StatusReserva.PENDENTE))
            .thenReturn(true);

        assertThatThrownBy(() -> service.reservar(EMAIL, 20L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ja tem uma reserva");
        verify(livroRepository, never()).decrementarEstoque(any());
    }

    @Test
    void reservar_rejeita_quando_aluno_atinge_o_limite() {
        Aluno aluno = aluno();
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuarioAluno(aluno)));
        when(alunoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(aluno));
        when(reservaRepository.existsByAlunoIdAndLivroIdAndStatus(any(), any(), any())).thenReturn(false);
        // 2 emprestimos ativos + 1 reserva pendente = 3 = limite
        when(emprestimoRepository.countByAlunoIdAndSituacao(10L, SituacaoEmprestimo.ATIVO)).thenReturn(2L);
        when(reservaRepository.countByAlunoIdAndStatus(10L, StatusReserva.PENDENTE)).thenReturn(1L);

        assertThatThrownBy(() -> service.reservar(EMAIL, 20L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("limite");
        verify(livroRepository, never()).decrementarEstoque(any());
    }

    @Test
    void reservar_rejeita_quando_aluno_tem_livro_em_atraso() {
        Aluno aluno = aluno();
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuarioAluno(aluno)));
        when(alunoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(aluno));
        when(reservaRepository.existsByAlunoIdAndLivroIdAndStatus(any(), any(), any())).thenReturn(false);
        when(emprestimoRepository.countAtrasadosByAluno(eq(10L), any())).thenReturn(1L);

        assertThatThrownBy(() -> service.reservar(EMAIL, 20L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("atraso");
        verify(livroRepository, never()).decrementarEstoque(any());
    }

    @Test
    void reservar_rejeita_quando_estoque_indisponivel() {
        Aluno aluno = aluno();
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuarioAluno(aluno)));
        when(alunoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(aluno));
        when(reservaRepository.existsByAlunoIdAndLivroIdAndStatus(any(), any(), any())).thenReturn(false);
        when(emprestimoRepository.countByAlunoIdAndSituacao(any(), any())).thenReturn(0L);
        when(reservaRepository.countByAlunoIdAndStatus(any(), any())).thenReturn(0L);
        when(livroRepository.decrementarEstoque(20L)).thenReturn(0);
        when(livroRepository.existsById(20L)).thenReturn(true);

        assertThatThrownBy(() -> service.reservar(EMAIL, 20L))
            .isInstanceOf(EstoqueIndisponivelException.class);
    }

    @Test
    void cancelar_marca_cancelada_e_libera_o_exemplar() {
        Reserva reserva = reservaPendente();
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuarioAluno(aluno())));
        when(reservaRepository.findByIdAndAlunoId(99L, 10L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        service.cancelar(EMAIL, 99L);

        assertThat(reserva.getStatus()).isEqualTo(StatusReserva.CANCELADA);
        verify(livroRepository).incrementarEstoque(20L);
    }

    @Test
    void confirmar_gera_emprestimo_e_nao_libera_estoque() {
        Reserva reserva = reservaPendente();
        Emprestimo emprestimo = Emprestimo.builder().id(500L).build();
        when(reservaRepository.findById(99L)).thenReturn(Optional.of(reserva));
        when(emprestimoService.registrarParaReserva(any(), any(), eq(7))).thenReturn(emprestimo);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservaResponse resp = service.confirmar(99L, 7);

        assertThat(resp.status()).isEqualTo(StatusReserva.CONFIRMADA);
        assertThat(reserva.getEmprestimo()).isSameAs(emprestimo);
        verify(emprestimoService).registrarParaReserva(any(), any(), eq(7));
        // confirmar NAO libera estoque — o exemplar passa da reserva para o emprestimo
        verify(livroRepository, never()).incrementarEstoque(any());
    }

    @Test
    void recusar_marca_recusada_e_libera_o_exemplar() {
        Reserva reserva = reservaPendente();
        when(reservaRepository.findById(99L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        service.recusar(99L);

        assertThat(reserva.getStatus()).isEqualTo(StatusReserva.RECUSADA);
        verify(livroRepository).incrementarEstoque(20L);
    }

    @Test
    void confirmar_rejeita_reserva_que_nao_esta_pendente() {
        Reserva reserva = reservaPendente();
        reserva.setStatus(StatusReserva.CANCELADA);
        when(reservaRepository.findById(99L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> service.confirmar(99L, 7))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("nao esta mais pendente");
    }
}
