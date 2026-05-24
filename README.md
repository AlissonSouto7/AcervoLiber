# 📚 AcervoLiber

> Sistema de gestão de biblioteca escolar — completo, gratuito e open source.

[![CI](https://github.com/AlissonSouto7/AcervoLiber/actions/workflows/ci.yml/badge.svg)](https://github.com/AlissonSouto7/AcervoLiber/actions/workflows/ci.yml)
[![Deploy GCP](https://github.com/AlissonSouto7/AcervoLiber/actions/workflows/deploy-gcp.yml/badge.svg)](https://github.com/AlissonSouto7/AcervoLiber/actions/workflows/deploy-gcp.yml)

**🌐 Demo em produção:** [acervoliber.duckdns.org](https://acervoliber.duckdns.org)

Desenvolvido como **trabalho comunitário** para a escola Gabriel José Pereira (Eunápolis-BA).

---

## ✨ O que faz?

Substitui aquela planilha do Excel ou caderno de empréstimos por um sistema web completo:

- 📖 **Cadastro do acervo** com capas automáticas (Google Books + Open Library)
- 👥 **Cadastro de alunos** com turma e matrícula
- 🔄 **Empréstimos** com semáforo de urgência (verde/amarelo/vermelho)
- 🔖 **Reservas pelos alunos** via portal próprio
- 🔐 **3 perfis** — Aluno (portal), Bibliotecário (gestão), Admin (controle total)
- 📊 **Dashboard** com livros mais emprestados, atrasados e alertas
- 📋 **Auditoria completa** de eventos de segurança (LGPD-friendly)

---

## 🎯 Quem usa?

### 👨‍🎓 Aluno
Loga com matrícula, navega no catálogo, reserva livros e acompanha "Minhas reservas".

### 📖 Bibliotecário
Cadastra livros/alunos, registra empréstimos e devoluções, gerencia reservas pendentes, renova/edita/cancela empréstimos.

### 👨‍💼 Admin (Diretor)
Tudo do bibliotecário + cria/desativa usuários da equipe + acessa a trilha de auditoria.

---

## 🛡️ Recursos de segurança

Sistema passou por **auditoria profunda** com 269 achados identificados em 7 fases, com 40 fixes aplicados:

- 🔐 **HTTPS automático** via Let's Encrypt
- 🎫 **JWT + Refresh token rotation** com reuse detection
- 🚫 **Account lockout** após 5 falhas + Rate limiting por IP
- 🛑 **Bloqueio empréstimo por atraso** — aluno com livro vencido não pega novo
- 🎭 **PII mascarada** em telas administrativas (LGPD §14, dados de menores)
- 🔍 **Auditoria de eventos** (logins, mudanças, reuso de token = sinal de roubo)
- 🧨 **XSS sanitization**, **CSP**, **HSTS**, **Permissions-Policy**, **COOP/CORP**

Detalhes em [`AUDIT_DEEP_SECURITY.md`](AUDIT_DEEP_SECURITY.md).

---

## 🏗️ Stack

| Camada | Tech |
|---|---|
| **Backend** | Spring Boot 4.0.6 · Java 17 · JPA/Hibernate · Flyway · Spring Security |
| **Banco** | PostgreSQL 16 (Neon na produção) |
| **Frontend** | React 18 + Vite + TypeScript + Ant Design + TanStack Query + Zustand |
| **Infra** | Docker · Docker Compose · Caddy 2 (TLS automático) |
| **Hospedagem** | GCP e2-micro (Always Free) + Neon Postgres (Free) + DuckDNS + Caddy/Let's Encrypt |
| **CI/CD** | GitHub Actions (build amd64 + push GHCR + deploy SSH) |

**Custo de hospedagem: R$ 0,00/mês** (100% free tier).

---

## 🚀 Rodando local (dev)

Pré-requisitos: Docker Desktop + Docker Compose v2.

```bash
# Clonar
git clone https://github.com/AlissonSouto7/AcervoLiber.git
cd AcervoLiber

# Volume do banco (uma vez)
docker volume create liber-pg-data

# Copiar template e ajustar
cp .env.example .env

# Subir o stack
docker compose up -d --build

# Acompanhar logs
docker compose logs -f app
```

**Acessos:**
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api/v1
- Swagger UI (só dev): http://localhost:8080/swagger-ui/index.html

**Credenciais de primeiro acesso:** se `ADMIN_PASSWORD` no `.env` estiver vazia, o `AdminSeeder` gera uma senha aleatória e loga uma única vez no startup. Procura em `docker compose logs app | grep "Senha do admin"`.

---

## 🌐 Deploy em produção

Veja [`docs/RUNBOOK.md`](docs/RUNBOOK.md) para o procedimento completo:

- Setup inicial da VM (Docker, firewall, swap)
- Configuração do `.env.prod` com secrets reais
- HTTPS automático via Caddy + Let's Encrypt + DuckDNS
- Backup e restore do banco
- Rotação de secrets
- Troubleshooting comum

**CI/CD:** `git push origin main` → build de imagens amd64 → push GHCR → SSH na VM → `docker compose pull && up -d` (~10 min).

---

## 📂 Estrutura

```
liber/
├── src/                          backend Spring Boot
│   ├── main/
│   │   ├── java/com/liber/
│   │   │   ├── config/           SecurityConfig, properties, etc.
│   │   │   ├── controller/       8 controllers REST
│   │   │   ├── service/          regras de negócio
│   │   │   ├── repository/       JPA repositories
│   │   │   ├── entity/           entidades JPA
│   │   │   ├── dto/              records de request/response
│   │   │   ├── security/         JwtService, filtros, lockout
│   │   │   └── seeder/           AdminSeeder + DadosExemploSeeder
│   │   └── resources/
│   │       ├── application*.properties
│   │       └── db/migration/     migrations Flyway V1..V15
│   └── test/                     68 testes unitários + 3 ITs (Testcontainers)
├── frontend/                     SPA React+Vite
│   ├── src/
│   │   ├── pages/                13 páginas
│   │   ├── api/                  clientes axios + TanStack queries
│   │   ├── auth/                 authStore (Zustand)
│   │   └── components/           AppLayout, ProtectedRoute, etc.
│   ├── nginx.conf                proxy /api → app:8080
│   └── Dockerfile
├── docker-compose.yml            stack base (dev local)
├── docker-compose.prod.yml       overlay produção Oracle ARM (com Postgres local)
├── docker-compose.gcp.yml        overlay produção GCP (sem Postgres, usa Neon)
├── Caddyfile                     reverse proxy + TLS automático
├── scripts/backup.sh             pg_dump diário rotacionando 30d
├── .github/workflows/            CI + CD
│   ├── ci.yml                    testes em PR e push
│   └── deploy-gcp.yml            deploy automático em push main
└── docs/RUNBOOK.md               operação em produção
```

---

## 🌿 Branches

- **`main`** — código em produção (CI/CD dispara deploy automático em cada push)
- **`dev`** — integração (recebe PRs das branches de feature)
- **`alisson`** — branch pessoal de trabalho

**Fluxo:** `alisson` → PR para `main` → CI verde → merge → deploy automático.

---

## 🧪 Testes

```bash
# Unitários (rápidos, sem Docker)
./mvnw test

# Inclui testes de integração (Testcontainers — precisa Docker)
./mvnw verify

# Frontend
cd frontend && npm run build   # typecheck + bundle
```

**Cobertura atual:**
- ✅ 68 testes unitários (Services + Security)
- ✅ 3 ITs end-to-end (admin flow + emprestimo flow + sanity)
- ❌ Sem `@WebMvcTest` ainda (controllers ainda sem cobertura)
- ❌ Sem testes de frontend ainda

---

## 📋 Roadmap

### ✅ Feito
- CRUD de Livros, Alunos, Empréstimos, Reservas
- Portal do Aluno (catálogo + reservas)
- Renovação, edição, cancelamento de empréstimo
- Bloqueio por atraso
- PII mascarada em listagens admin
- Dashboard + Auditoria
- HTTPS + CI/CD
- Migration V1 → V15 (estável)

### 🟡 Próximo (P1)
- Relatórios CSV (lista de atrasados, inventário, devedores)
- Notificação ao aluno (badge in-app quando reserva confirmada)
- Testes `@WebMvcTest` nos controllers
- Lint do frontend (ESLint + Prettier)

### 🔵 Futuro (P2)
- Gestão de turmas como entidade (com promoção fim de ano)
- Busca avançada de livros (filtro gênero, "só disponíveis")
- Acessibilidade (WCAG)
- Observabilidade (métricas de negócio + tracing)

Veja [`AUDIT_PENDENCIAS.md`](AUDIT_PENDENCIAS.md) pra lista completa.

---

## 🤝 Contribuindo

PRs welcome! Algumas regras:

1. **Branch a partir de `main`**, mas faz PR pra `dev` (não direto pra main)
2. **Migration Flyway**: nunca edite uma já aplicada — crie nova com versão maior
3. **Commits** seguem [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `chore:`, `docs:`)
4. **Testes** obrigatórios em código novo de Service/Controller
5. **PII**: nunca exponha matrícula/nome de aluno completo em listagens admin sem mascarar

---

## 🐛 Reportar vulnerabilidade

Por favor **não abra issue pública**. Use [GitHub Security Advisories](https://github.com/AlissonSouto7/AcervoLiber/security/advisories/new) (privado).

---

## 📜 Licença

Projeto educacional/comunitário. Use à vontade, atribua quando possível.

---

## 💌 Créditos

Sistema concebido e desenvolvido por **Alisson Pinheiro Souto** ([@AlissonSouto7](https://github.com/AlissonSouto7)) para a escola Gabriel José Pereira (Eunápolis-BA, 2026).

Com auxílio de Claude (Anthropic) em 3 áreas: auditoria de segurança profunda, arquitetura de produção e setup de infra/CI-CD.
