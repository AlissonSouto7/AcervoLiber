-- ============================================================================
-- V3__add_password_changed_at_to_usuarios.sql
-- ============================================================================
-- Permite invalidar JWTs antigos apos troca de senha — filtro compara `iat` do
-- token com este timestamp e rejeita se for anterior.
-- DEFAULT NOW() preenche usuarios existentes (admin seedado).
-- ============================================================================

ALTER TABLE usuarios
    ADD COLUMN password_changed_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW();
