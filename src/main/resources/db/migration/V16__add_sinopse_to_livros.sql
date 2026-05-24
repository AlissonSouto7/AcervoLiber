-- ============================================================================
-- V16__add_sinopse_to_livros.sql
-- ============================================================================
-- Sinopse do livro, mostrada ao aluno no modal "Ver detalhes" e editavel
-- pelo bibliotecario/admin no formulario de livro.
-- Nullable: livros antigos ficam sem sinopse; o backfill (Google Books) tenta
-- preencher automaticamente em background, e o bibliotecario pode editar.
-- ============================================================================

ALTER TABLE livros ADD COLUMN sinopse VARCHAR(2000);
