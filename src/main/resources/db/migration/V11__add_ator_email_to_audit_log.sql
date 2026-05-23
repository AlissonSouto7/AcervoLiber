-- =============================================================================
-- V11 — adiciona coluna `ator_email` em audit_log (quem executou a acao)
-- =============================================================================
-- Antes desta coluna, a trilha registrava apenas o ALVO da acao (em usuario_email).
-- Em ataques de insider — admin comprometido desativa outro admin — a trilha
-- apontava para a vitima, sem identificar o autor. Forense impossivel.
-- ator_email captura o sujeito autenticado no momento (SecurityContextHolder).
-- Nullable: eventos sem ator autenticado (LOGIN_FALHA, REFRESH_REUSO) ficam NULL.
-- =============================================================================

ALTER TABLE audit_log ADD COLUMN ator_email VARCHAR(150);

CREATE INDEX idx_audit_log_ator_email ON audit_log (ator_email);
