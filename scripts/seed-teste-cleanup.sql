-- ============================================================================
-- seed-teste-cleanup.sql — LIMPA dados de teste mantendo admin ativo
-- ============================================================================
-- Use ANTES de entregar o sistema pra escola, pra zerar tudo de teste.
--
-- Operações em ordem para respeitar foreign keys:
-- 1. Emprestimos (FK pra livros + alunos)
-- 2. Reservas (FK pra livros + alunos + emprestimos)
-- 3. Refresh tokens (FK pra usuarios — limpa sessoes de alunos teste)
-- 4. Audit log (limpa rastros dos testes; admin nao se importa)
-- 5. Livro_capa (FK pra livros)
-- 6. Livros
-- 7. Usuarios role=ALUNO (mantem ADMIN/BIBLIOTECARIO)
-- 8. Alunos
--
-- Resultado: banco zerado mantendo apenas:
-- - O usuario admin (e bibliotecarios criados que NAO sao role=ALUNO)
-- - Migrations (flyway_schema_history)
-- ============================================================================

BEGIN;

DELETE FROM emprestimos;
DELETE FROM reservas;
DELETE FROM refresh_tokens WHERE usuario_id IN (SELECT id FROM usuarios WHERE role = 'ALUNO');
DELETE FROM audit_log;
DELETE FROM livro_capa;
DELETE FROM livros;
DELETE FROM usuarios WHERE role = 'ALUNO';
DELETE FROM alunos;

-- Reset das sequencias pra reiniciar IDs em 1
ALTER SEQUENCE livros_id_seq RESTART WITH 1;
ALTER SEQUENCE alunos_id_seq RESTART WITH 1;
ALTER SEQUENCE emprestimos_id_seq RESTART WITH 1;
ALTER SEQUENCE reservas_id_seq RESTART WITH 1;

-- Verificacao final
SELECT 'livros restantes' AS info, COUNT(*) AS qtd FROM livros
UNION ALL
SELECT 'alunos restantes', COUNT(*) FROM alunos
UNION ALL
SELECT 'emprestimos restantes', COUNT(*) FROM emprestimos
UNION ALL
SELECT 'reservas restantes', COUNT(*) FROM reservas
UNION ALL
SELECT 'usuarios admin/bib restantes', COUNT(*) FROM usuarios WHERE role IN ('ADMIN', 'BIBLIOTECARIO');

COMMIT;
