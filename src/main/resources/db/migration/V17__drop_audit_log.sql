-- ============================================================================
-- V17__drop_audit_log.sql
-- ============================================================================
-- Remove a tabela audit_log e o sistema de auditoria do produto.
-- Decisao do dono do projeto: a tela de Auditoria nao era usada pela escola e
-- a complexidade extra (entity, service, controller, page) nao se pagava.
-- Eventos de seguranca importantes continuam aparecendo no log da aplicacao
-- (Logback JSON) — quem precisar investigar incidente checa os logs do app.
-- ============================================================================

DROP TABLE IF EXISTS audit_log;
