package com.liber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.liber.dto.LivroRequest;
import com.liber.dto.LivroResponse;
import com.liber.entity.Livro;
import com.liber.entity.SituacaoEmprestimo;
import com.liber.exception.BusinessException;
import com.liber.exception.RegraEmprestimoException;
import com.liber.exception.ResourceNotFoundException;
import com.liber.repository.EmprestimoRepository;
import com.liber.repository.LivroRepository;
import com.liber.repository.ReservaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LivroServiceTest {

    @Mock LivroRepository livroRepository;
    @Mock EmprestimoRepository emprestimoRepository;
    @Mock ReservaRepository reservaRepository;
    @Mock CapaService capaService;
    @InjectMocks LivroService service;

    @Test
    void cadastrar_inicializa_quantidade_disponivel_igual_a_exemplares() {
        LivroRequest req = new LivroRequest("Dom Casmurro", "Machado de Assis", null, 1899, 5);
        when(livroRepository.save(any(Livro.class))).thenAnswer(inv -> inv.getArgument(0));

        LivroResponse resp = service.cadastrar(req);

        assertThat(resp.quantidadeExemplares()).isEqualTo(5);
        assertThat(resp.quantidadeDisponivel()).isEqualTo(5);
    }

    @Test
    void cadastrar_rejeita_isbn_duplicado() {
        LivroRequest req = new LivroRequest("X", "Y", "9788535914849", 2020, 3);
        when(livroRepository.existsByIsbn("9788535914849")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(req))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ISBN ja cadastrado");
    }

    @Test
    void atualizar_recalcula_disponivel_a_partir_dos_ativos() {
        // Estado: 10 exemplares, 7 disponiveis, 3 ativos (invariante ok)
        Livro existente = Livro.builder()
            .id(1L).titulo("X").autor("Y")
            .quantidadeExemplares(10).quantidadeDisponivel(7)
            .build();
        when(livroRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(emprestimoRepository.countByLivroIdAndSituacao(1L, SituacaoEmprestimo.ATIVO))
            .thenReturn(3L);
        when(livroRepository.save(any(Livro.class))).thenAnswer(inv -> inv.getArgument(0));

        // Reduz para 5 exemplares
        LivroRequest req = new LivroRequest("X", "Y", null, 2020, 5);
        LivroResponse resp = service.atualizar(1L, req);

        assertThat(resp.quantidadeExemplares()).isEqualTo(5);
        // 5 total - 3 ativos = 2 disponiveis (calc direto, nao via diff)
        assertThat(resp.quantidadeDisponivel()).isEqualTo(2);
    }

    @Test
    void atualizar_rejeita_quando_novo_total_e_menor_que_ativos() {
        Livro existente = Livro.builder()
            .id(1L).titulo("X").autor("Y")
            .quantidadeExemplares(10).quantidadeDisponivel(7)
            .build();
        when(livroRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(emprestimoRepository.countByLivroIdAndSituacao(1L, SituacaoEmprestimo.ATIVO))
            .thenReturn(5L);

        // Tenta reduzir para 3 — mas ha 5 ativos, deve falhar
        LivroRequest req = new LivroRequest("X", "Y", null, 2020, 3);

        assertThatThrownBy(() -> service.atualizar(1L, req))
            .isInstanceOf(RegraEmprestimoException.class);
    }

    @Test
    void remover_rejeita_quando_existe_historico_de_emprestimos() {
        when(livroRepository.existsById(1L)).thenReturn(true);
        when(emprestimoRepository.existsByLivroId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.remover(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("historico");
    }

    @Test
    void remover_inexistente_lanca_NotFound() {
        when(livroRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.remover(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
