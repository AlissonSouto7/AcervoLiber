-- ============================================================================
-- V8__add_capa_url_to_livros.sql
-- ============================================================================
-- Guarda a URL da capa do livro (resolvida pelo backend via Google Books a
-- partir do ISBN). Cacheia o resultado no banco para o frontend nunca precisar
-- consultar a Google Books diretamente.
-- Nullable: livro sem ISBN, ou ISBN sem capa conhecida, fica com capa_url NULL.
-- ============================================================================

ALTER TABLE livros ADD COLUMN capa_url VARCHAR(500);
