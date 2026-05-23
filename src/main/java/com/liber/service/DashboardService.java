package com.liber.service;

import com.liber.dto.DashboardAlertaDTO;
import com.liber.dto.DashboardResponse;
import com.liber.dto.LivroRankingDTO;
import com.liber.dto.StatusUrgencia;
import com.liber.entity.Emprestimo;
import com.liber.entity.SituacaoEmprestimo;
import com.liber.repository.AlunoRepository;
import com.liber.repository.EmprestimoRepository;
import com.liber.repository.LivroRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int TOP_RANKING = 10;

    private final LivroRepository livroRepository;
    private final AlunoRepository alunoRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final Clock clock;

    public DashboardResponse obter() {
        LocalDate hoje = LocalDate.now(clock);

        DashboardResponse.Totais totais = new DashboardResponse.Totais(
            livroRepository.count(),
            alunoRepository.count(),
            emprestimoRepository.countBySituacao(SituacaoEmprestimo.ATIVO),
            emprestimoRepository.countAtrasados(hoje)
        );

        // Itera os emprestimos ATIVO direto e converte para DashboardAlertaDTO
        // (matricula mascarada — Fase 7 fix). Calcula o statusUrgencia inline,
        // sem materializar EmprestimoResponse intermediario.
        List<Emprestimo> ativos = emprestimoRepository
            .findBySituacaoOrderByDataDevolucaoPrevistaAsc(SituacaoEmprestimo.ATIVO);

        List<DashboardAlertaDTO> proximos = ativos.stream()
            .filter(e -> StatusUrgencia.from(e, hoje) == StatusUrgencia.AMARELO)
            .map(e -> DashboardAlertaDTO.from(e, hoje))
            .toList();

        List<DashboardAlertaDTO> atrasados = ativos.stream()
            .filter(e -> StatusUrgencia.from(e, hoje) == StatusUrgencia.VERMELHO)
            .map(e -> DashboardAlertaDTO.from(e, hoje))
            .toList();

        List<LivroRankingDTO> ranking = emprestimoRepository
            .rankingLivrosMaisEmprestados(PageRequest.of(0, TOP_RANKING));

        return new DashboardResponse(totais, proximos, atrasados, ranking);
    }
}
