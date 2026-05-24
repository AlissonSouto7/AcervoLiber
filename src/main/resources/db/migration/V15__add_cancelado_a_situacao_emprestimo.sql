-- Adiciona o valor CANCELADO ao CHECK de situacao do emprestimo.
-- Usado pelo endpoint DELETE /emprestimos/{id} (cancelamento de lancamento errado):
-- o emprestimo nao e deletado fisicamente (preserva FK de reservas confirmadas e
-- trilha de auditoria) — apenas muda situacao=CANCELADO e devolve o livro ao estoque.

ALTER TABLE emprestimos
    DROP CONSTRAINT ck_emprestimos_situacao;

ALTER TABLE emprestimos
    ADD CONSTRAINT ck_emprestimos_situacao
    CHECK (situacao IN ('ATIVO', 'DEVOLVIDO', 'CANCELADO'));
