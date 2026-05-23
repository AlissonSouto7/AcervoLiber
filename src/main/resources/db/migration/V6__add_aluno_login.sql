-- ============================================================================
-- V6__add_aluno_login.sql
-- ============================================================================
-- Permite que alunos tenham acesso ao sistema (portal do aluno):
--  - usuarios.aluno_id           : vincula um Usuario (role ALUNO) ao registro Aluno
--  - usuarios.deve_trocar_senha  : senha provisoria — exige troca no primeiro acesso
-- ============================================================================

ALTER TABLE usuarios ADD COLUMN aluno_id BIGINT;
ALTER TABLE usuarios ADD COLUMN deve_trocar_senha BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE usuarios
    ADD CONSTRAINT fk_usuarios_aluno FOREIGN KEY (aluno_id) REFERENCES alunos (id);

-- Um aluno tem no maximo um acesso (multiplos NULL sao permitidos para a equipe)
ALTER TABLE usuarios
    ADD CONSTRAINT uk_usuarios_aluno UNIQUE (aluno_id);
