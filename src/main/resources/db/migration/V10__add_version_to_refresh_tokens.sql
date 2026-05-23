-- =============================================================================
-- V10 — adiciona coluna `version` em refresh_tokens (locking otimista)
-- =============================================================================
-- Protege a rotacao de refresh tokens contra race condition: sem @Version, duas
-- requisicoes paralelas com o mesmo refresh podem emitir dois filhos validos
-- do mesmo pai (mascarando deteccao de reuso). Com @Version, a perdedora estoura
-- OptimisticLockException → 409 e o cliente retenta.
-- =============================================================================

ALTER TABLE refresh_tokens ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
