-- Adiciona contador de renovacoes ao emprestimo.
-- Default 0 (registros existentes sao tratados como nunca renovados).
-- O limite e enforced em codigo via EmprestimoProperties.maxRenovacoes (default 2).
ALTER TABLE emprestimos
    ADD COLUMN renovacoes INTEGER NOT NULL DEFAULT 0;

-- Sanity: nao deve ser negativo (mesma intencao do @Min(0) em codigo)
ALTER TABLE emprestimos
    ADD CONSTRAINT ck_emprestimos_renovacoes_positivo CHECK (renovacoes >= 0);
