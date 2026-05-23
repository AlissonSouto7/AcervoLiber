-- ============================================================================
-- V12__add_unique_reserva_pendente.sql
-- ============================================================================
-- Defesa em DB contra duas reservas pendentes do mesmo aluno no mesmo livro.
-- O ReservaService ja serializa concorrencia da mesma sessao via
-- alunoRepository.findByIdForUpdate(alunoId), mas dois clientes do mesmo aluno
-- (web + app, ou racing tabs sem o lock futuro), ou um bypass via SQL direto,
-- conseguiriam furar a check do servico. Este UNIQUE parcial fecha o caso na
-- borda do DB — qualquer tentativa de duplicar cai com erro de integridade,
-- traduzido para HTTP 422 no GlobalExceptionHandler.
-- ============================================================================

CREATE UNIQUE INDEX uq_reservas_aluno_livro_pendente
    ON reservas (aluno_id, livro_id)
    WHERE status = 'PENDENTE';
