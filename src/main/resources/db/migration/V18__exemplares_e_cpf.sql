-- ============================================================================
-- V18__exemplares_e_cpf.sql
-- ============================================================================
-- Reestruturacao pre-entrega a escola Gabriel Jose Pereira:
--
-- 1) Cada copia fisica do livro vira um Exemplar com codigo proprio (numero
--    de tombamento). Empr e devolucao passam a ser por exemplar, nao por
--    "livro generico" + contador.
--
-- 2) Aluno e identificado por CPF (validado com digito verificador) — a escola
--    nao tem matricula formal pros alunos. Login do aluno tambem por CPF.
--
-- 3) Banco e LIMPO antes de mexer no schema — alinhado com a decisao do dono
--    do projeto de entregar zerado a escola. Em prod, o admin atual e mantido.
--
-- ATENCAO: e destrutiva pra dados de teste, intencional. Adminstrador
-- preservado. Esta migration roda APENAS uma vez em prod e marca o ponto
-- de virada do modelo.
-- ============================================================================

-- ---------- 1) Limpa dados antigos ------------------------------------------
-- Ordem importa: tabelas dependentes (com FK) sao deletadas ANTES das
-- dependencias. Reservas referencia emprestimos (fk_reservas_emprestimo), entao
-- reservas vai primeiro. Usuarios referencia alunos (aluno_id), entao usuarios
-- antes de alunos.
DELETE FROM reservas;
DELETE FROM emprestimos;
DELETE FROM refresh_tokens WHERE usuario_id IN (SELECT id FROM usuarios WHERE role != 'ADMIN');
DELETE FROM livro_capa;
DELETE FROM livros;
DELETE FROM usuarios WHERE role != 'ADMIN';
DELETE FROM alunos;

-- ---------- 2) Aluno: matricula -> cpf -------------------------------------
ALTER TABLE alunos DROP COLUMN matricula;
ALTER TABLE alunos ADD COLUMN cpf VARCHAR(11) NOT NULL UNIQUE;

-- ---------- 3) Livro: dropa contadores (derivados de exemplares) -----------
ALTER TABLE livros DROP COLUMN quantidade_exemplares;
ALTER TABLE livros DROP COLUMN quantidade_disponivel;

-- ---------- 4) Tabela exemplares -------------------------------------------
-- A unicidade do codigo e por todo o sistema, nao por livro: o codigo bate
-- com a etiqueta de tombamento que a escola cola no livro fisico. Dois livros
-- diferentes NUNCA tem o mesmo codigo.
--
-- Situacoes:
--   DISPONIVEL  - na prateleira, pronto pra emprestar
--   EMPRESTADO  - com um aluno
--   RESERVADO   - segurado por uma reserva pendente
--   EXTRAVIADO  - bibliotecario marcou como perdido (visivel mas nao emprestavel)
CREATE TABLE exemplares (
    id            BIGSERIAL    PRIMARY KEY,
    livro_id      BIGINT       NOT NULL REFERENCES livros (id) ON DELETE RESTRICT,
    codigo        VARCHAR(50)  NOT NULL UNIQUE,
    situacao      VARCHAR(20)  NOT NULL DEFAULT 'DISPONIVEL'
                  CHECK (situacao IN ('DISPONIVEL', 'EMPRESTADO', 'RESERVADO', 'EXTRAVIADO')),
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_exemplares_livro_id ON exemplares (livro_id);
CREATE INDEX idx_exemplares_situacao ON exemplares (situacao);

-- ---------- 5) Sequence pro codigo padrao (LIB-XXXX) ------------------------
-- O bibliotecario pode editar o codigo pra bater com a etiqueta fisica que a
-- escola ja tem. Mas no cadastro normal, o sistema gera o proximo da sequence
-- com prefixo "LIB-" e padding de 5 digitos. Ex.: LIB-00001, LIB-00002.
CREATE SEQUENCE exemplar_codigo_seq START WITH 1 INCREMENT BY 1;

-- ---------- 6) Emprestimo: livro_id -> exemplar_id --------------------------
-- O empr passa a referenciar o exemplar especifico (codigo de tombamento).
-- Saber que o aluno X pegou o exemplar LIB-00042 e fundamental pra escola:
-- se ele extraviar, sabemos exatamente qual livro fisico ficou faltando.
ALTER TABLE emprestimos DROP COLUMN livro_id;
ALTER TABLE emprestimos ADD COLUMN exemplar_id BIGINT NOT NULL
    REFERENCES exemplares (id) ON DELETE RESTRICT;
CREATE INDEX idx_emprestimos_exemplar ON emprestimos (exemplar_id);

-- ---------- 7) Reservas: mantem livro_id (reserva e pelo titulo) ------------
-- Aluno reserva o LIVRO (qualquer exemplar serve). Na confirmacao, o sistema
-- atribui um exemplar disponivel. Adicionamos exemplar_id NULLABLE pra
-- guardar qual exemplar acabou sendo usado quando a reserva e confirmada.
ALTER TABLE reservas ADD COLUMN exemplar_id BIGINT
    REFERENCES exemplares (id) ON DELETE RESTRICT;
CREATE INDEX idx_reservas_exemplar ON reservas (exemplar_id);
