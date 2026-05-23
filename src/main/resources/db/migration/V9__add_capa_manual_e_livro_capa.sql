-- ============================================================================
-- V9__add_capa_manual_e_livro_capa.sql
-- ============================================================================
-- Permite que o admin/bibliotecario envie uma capa propria para o livro.
--
--  - livros.capa_manual: marca que a capa foi definida manualmente. Quando true,
--    a resolucao automatica (Google Books / Open Library) NAO sobrescreve a capa.
--  - livro_capa: guarda os bytes da imagem enviada. Tabela separada de propósito
--    — assim a listagem de livros nao carrega o binario da capa em toda query.
-- ============================================================================

ALTER TABLE livros ADD COLUMN capa_manual BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE livro_capa (
    livro_id      BIGINT PRIMARY KEY REFERENCES livros (id) ON DELETE CASCADE,
    imagem        BYTEA        NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    atualizado_em TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
