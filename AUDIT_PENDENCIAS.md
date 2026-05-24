# Audit de Pendências — O que falta fazer — AcervoLiber

> Status atualizado: **2026-05-24** — sistema em produção em https://acervoliber.duckdns.org (GCP e2-micro + Neon Postgres).
> Prioridades: P0 bloqueia produção · P1 importante · P2 desejável · P3 nice-to-have.
> Legenda: ✅ feito · 🟡 parcial · ❌ pendente

---

## P0 — Bloqueiam produção

### ✅ Projeto versionado e CI rodando
Repo `AlissonSouto7/AcervoLiber` no GitHub (público). Branches: `main`, `dev`, `alisson`. CI (`.github/workflows/ci.yml`) já rodou e os 3 IT (`AdminFlowIT`, `EmprestimoFlowIT`, `LiberApplicationIT`) passam. 68 testes unitários verdes.

### ✅ HTTPS/TLS terminado
Caddy 2 na borda fazendo TLS automático via Let's Encrypt no domínio `acervoliber.duckdns.org`. Headers de segurança (HSTS, Permissions-Policy, COOP, CORP, `Cache-Control: no-store` em `/index.html`) adicionados no `Caddyfile`. Cobre achados da Fase 4.D / 6.B do AUDIT_DEEP_SECURITY.

### 🟡 Backup do banco
**Em produção (GCP+Neon):** Neon free tier tem **snapshot automático com PITR (point-in-time-recovery) de 7 dias** — cobre acidente operacional comum. **Mas não há backup off-site nem retenção longa.** TODO: setup de `pg_dump` diário via cron na VM contra Neon, upload pra Backblaze B2 ou GCS (também free tier).
**Setup local (Oracle/postgres local):** `scripts/backup.sh` + serviço `backup` no `docker-compose.prod.yml` cobre pg_dump diário com rotação 30d (não usado em prod GCP).

---

## P1 — Importantes antes de entregar

### ✅ Funcionalidades de biblioteca

- ✅ **Renovação de empréstimo** (`POST /emprestimos/{id}/renovacao`) — commit `ac71c36`. Limite configurável via `app.emprestimo.max-renovacoes` (default 2), bloqueio por atraso, limite atingido, reserva pendente de outro aluno.
- ✅ **Bloqueio por atraso** — commit `ac71c36`. Aluno com livro em atraso não pega novo nem reserva. Mensagem clara no UI.
- ✅ **Edição/cancelamento de empréstimo** (`PATCH /emprestimos/{id}` + `DELETE /emprestimos/{id}`) — commit `00732f0`. PATCH edita data/prazo, DELETE marca como CANCELADO (soft delete preservando FK de reservas confirmadas) e devolve livro ao estoque. Novo enum `SituacaoEmprestimo.CANCELADO` + migration V15.

### 🟡 Testes
- ✅ Testes unitários novos para todos os comportamentos adicionados (bloqueio atraso, renovação, edição, cancelamento — 7 testes novos).
- ❌ Ainda sem teste unitário: `DashboardService`, `AlunoService`, `AuthService`, `UsuarioService`, `CapaService`, `AdminSeeder`, `SenhaForteValidator`, e todos os 9 controllers (nenhum `@WebMvcTest`). `AuthService`/`UsuarioService` continuam sem cobertura — risco real porque são código de segurança.

### ✅ Documentação
- ✅ **README.md na raiz** — stack, como rodar local, variáveis de ambiente, credenciais primeiro acesso, estrutura do projeto, branches, comandos de teste.
- ✅ **`docs/RUNBOOK.md`** — operação completa: deploy, backup/restore, criar bibliotecário, rotacionar `JWT_SECRET` / `POSTGRES_PASSWORD`, destravar conta admin via SQL, troubleshooting comum, checklist mensal. Ajustado pra Oracle Linux 9 (`dnf`, `opc`, `firewalld`) no commit `1fa7cf8` — em produção atual usamos Ubuntu 26.04 LTS Minimal na GCP, comandos `apt` equivalentes.

### 🟡 CI/CD
- ✅ **CI** roda em PRs e push para `main` (`mvnw verify` + frontend build).
- 🟡 **CD** existe em `.github/workflows/deploy.yml` mas mira **Oracle ARM** (build buildx ARM64, push GHCR, SSH em VM Oracle com `opc`). **Precisa adaptar pra GCP**: build amd64, SSH no usuário Google (`alissonprogp1`) na VM `acervo-liber1`, usar `docker-compose.gcp.yml`.
- ❌ Lint do frontend (ESLint) — nunca configurado.
- ❌ Gate de cobertura (JaCoCo) — não configurado.

### ✅ Edge case `incrementarEstoque`
Quando `incrementarEstoque` na devolução afeta 0 linhas, agora registra evento `ESTOQUE_DIVERGENCIA` na auditoria (não só log.warn). Aplicado nas Fases 3 e cancelamento de empréstimo.

---

## P2 — Desejáveis

- ❌ **Gestão de turmas / ano letivo** — `turma` continua string livre no `Aluno`. Sem entidade Turma nem promoção/arquivamento. Esforço grande.
- ❌ **Relatórios / exportação CSV/PDF** — bibliotecário não consegue imprimir lista de atrasados, devedores, inventário. Esforço médio.
- ❌ **JaCoCo** — nenhum plugin configurado, sem mínimo de cobertura.
- ❌ **Testes de frontend** — zero (sem Vitest/Jest). Interceptor de refresh + authStore sem cobertura.
- ❌ **Error Boundary no frontend** — `App.tsx` sem nenhum; erro de render derruba SPA pra tela branca.
- ❌ **Observabilidade** — `/actuator/metrics` exposto, sem métricas de negócio nem tracing, logs JSON sem destino externo.
- ❌ **Reserva confirmada não revalida limite do aluno** — `ReservaService.confirmar`. Trivial.
- 🟡 **Rate limit / lockout só in-memory** — funciona em instância única (nosso caso GCP), mas se escalar pra 2+ VMs precisa Redis. Documentado no RUNBOOK.
- ✅ **Senha default fraca no compose** — `app.admin.password=${ADMIN_PASSWORD:}` default vazio + `AdminSeeder` gera aleatória + loga uma vez. Validado em prod (a admin atual veio assim: `qpLJZ4CyGPxmf1rFIAV5`).

---

## P3 — Nice-to-have

- ❌ **Notificação ao aluno** — reserva confirmada/recusada e atraso são mudos. Esforço: pequeno (badge in-app) / médio (e-mail via Neon não tem; GCP free tier não tem email; precisaria SendGrid free).
- ❌ **Busca avançada** — busca é único `termo` com `LIKE`; sem filtro por gênero/só disponíveis/ordenação.
- ❌ **Acessibilidade** — não verificada (contraste, teclado, `aria-*`).
- ❌ **Validações de domínio no frontend** — ISBN/matrícula só validado no backend (hoje server valida com 422 amigável).
- ❌ **i18n** — PT-BR hard-coded. Aceitável pra escola brasileira.
- ❌ **`JAVA_OPTS`** ajustável via compose — em prod GCP já passamos via env no `docker-compose.gcp.yml` (`MaxRAMPercentage=40 + SerialGC` pra caber em e2-micro).

---

## Itens novos pós-deploy GCP (não estavam no audit original)

### 🟡 Rotação de secrets que passaram pelo chat
- `JWT_SECRET` (`ssd8RrAJ...`) — gerado nesta sessão, **passou pelo chat**. Recomendado rotacionar pelo menos 1x após deploy.
- Senha Neon (`npg_vKDf...`) — gerada na criação do projeto Neon, **passou pelo chat**. Recomendado rotacionar pelo Dashboard Neon → Reset password.
- Senha admin auto (`qpLJZ4Cy...`) — gerada pelo `AdminSeeder` no boot, **passou pelo log + chat**. Aluno deve trocar via UI no primeiro login.

### 🟡 IP da VM GCP é ephemeral
Atual `35.209.237.6`. Se a VM for parada/iniciada, IP MUDA. Soluções:
1. Aceitar e atualizar DuckDNS manualmente quando acontecer (cron `curl` resolve)
2. Promover IP pra "static" no GCP (NÃO está no Always Free — pode cobrar ~$2-7/mês)

### ❌ Backup Neon off-site
Neon free tier tem PITR de 7 dias só. Pra retention longa: `pg_dump` diário na VM GCP → upload Backblaze B2 (10 GB free) ou GCS bucket (5 GB free).

### ❌ Plano de migração se Oracle ARM liberar
O loop PowerShell ficou rodando 17h sem capacidade. Se um dia liberar, vale migrar pra Oracle (24 GB RAM vs 1 GB GCP, Postgres local vs Neon cold start). Plano: spin Oracle, copiar dados via `pg_dump | psql`, trocar DuckDNS.

---

## Resumo executivo — status real em produção (2026-05-24)

### ✅ Pronto / Deployed
- Sistema rodando em https://acervoliber.duckdns.org com HTTPS
- Backend Spring Boot 4.0.6 + Postgres 16 (Neon) + frontend React/Vite + Caddy TLS
- Renovação, bloqueio por atraso, edição/cancelamento de empréstimo
- PII de menores mascarada em DTOs admin (LGPD)
- 269 achados do AUDIT_DEEP_SECURITY com 31 fixes aplicados ao vivo + 7 fixes pós-deploy

### 🟡 Mitigações ativas
- 1 GB RAM da e2-micro = swap 2GB criado + Hikari pool 5 conexões + JAVA_OPTS reduzido
- Neon free tier = 0.5 GB storage + cold start 2s + suspende após 5min idle
- Latência Brasil→GCP us-central1 = ~150ms (perceptível mas aceitável)

### ❌ Pendente real (priorização sugerida)
1. **Rotacionar secrets** que passaram pelo chat (admin, JWT, Neon)
2. **CI/CD GCP** — adaptar `deploy.yml` que está mirando Oracle
3. **Backup Neon off-site** — `pg_dump` cron + B2/GCS
4. **Testes unitários** AuthService, UsuarioService, controllers
5. **Relatórios CSV** lista de atrasados / inventário

As lacunas restantes são de **qualidade interna** (testes, observabilidade) e **completude funcional** (relatórios, turmas). A base operacional está sólida.
