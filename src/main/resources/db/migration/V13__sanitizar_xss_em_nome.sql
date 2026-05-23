-- ============================================================================
-- V13__sanitizar_xss_em_nome.sql
-- ============================================================================
-- One-shot: remove caracteres < e > de nomes de usuario que foram aceitos
-- antes do @Pattern entrar em vigor. O pentest da Fase 7 reproduziu
-- "<script>alert(1)</script>" sendo persistido — React escapa na renderizacao,
-- mas o payload polui logs/auditoria/relatorios e quebra futuras telas que
-- usem dangerouslySetInnerHTML ou renderem nome em HTML (emails, PDF).
-- Migration roda 1 vez; novos inserts/updates ja sao validados pelo Pattern
-- do AtualizarPerfilRequest e CriarUsuarioRequest.
-- ============================================================================

UPDATE usuarios
SET nome = REGEXP_REPLACE(nome, '[<>]', '', 'g')
WHERE nome ~ '[<>]';
