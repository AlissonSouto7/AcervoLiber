-- ============================================================================
-- V2__add_version_to_emprestimos.sql
-- ============================================================================
-- Adiciona coluna `version` em emprestimos para optimistic locking via JPA.
-- Previne double-return concorrente (dois clicks simultaneos no botao "devolver").
-- DEFAULT 0 preenche linhas existentes; novos inserts sao gerenciados pelo
-- Hibernate via @Version.
-- ============================================================================

ALTER TABLE emprestimos ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
