# 📚 AcervoLiber

> Sistema de gestão de biblioteca escolar — gratuito, open source, hospedado em cloud free tier.

[![CI](https://github.com/AlissonSouto7/AcervoLiber/actions/workflows/ci.yml/badge.svg)](https://github.com/AlissonSouto7/AcervoLiber/actions/workflows/ci.yml)
[![Deploy GCP](https://github.com/AlissonSouto7/AcervoLiber/actions/workflows/deploy-gcp.yml/badge.svg)](https://github.com/AlissonSouto7/AcervoLiber/actions/workflows/deploy-gcp.yml)
[![License](https://img.shields.io/badge/license-MIT--like-blue)](#-licença)

**🌐 Demo em produção:** [acervoliber.duckdns.org](https://acervoliber.duckdns.org)

Desenvolvido como **trabalho comunitário** para a **Escola Municipal Gabriel José Pereira** (Eunápolis-BA), substituindo o controle manual em cadernos por um sistema web moderno.

---

## ✨ O que faz

Substitui o caderno de empréstimos e a planilha de acervo por um sistema web acessível em qualquer dispositivo (computador, tablet ou celular):

- 📖 **Cadastro do acervo** com capa automática (Google Books + Open Library) e sinopse
- 🏷️ **Exemplares com código de tombamento** — cada cópia física tem identidade própria (`LIB-00042`); empréstimo é por exemplar
- 👥 **Cadastro de alunos** por **CPF** (com validação de dígito verificador, máscara automática, dado oficial que aluno e família conhecem)
- 🔄 **Empréstimos** com prazo, devolução, renovação (até 2x), edição e cancelamento
- 🚦 **Semáforo de urgência** (verde / amarelo / vermelho) por proximidade do vencimento
- 🛑 **Bloqueio automático** de aluno em atraso
- 🔖 **Reservas online** pelo aluno — segura o exemplar, bibliotecário confirma no balcão
- 🔐 **3 perfis distintos** — Aluno (portal), Bibliotecário (gestão), Admin (controle total)
- 📊 **Dashboard** com livros mais emprestados, atrasados, próximos a vencer
- 🪪 **PII mascarada** em listas administrativas (LGPD §14 — dados de menores)
- 📄 **Manual em PDF** entregue pra escola ([docs/MANUAL-PROFESSOR.pdf](docs/MANUAL-PROFESSOR.pdf))

---

## 🎯 Quem usa

### 👨‍🎓 Aluno
Loga com **CPF** (com máscara automática), navega no catálogo, vê detalhes do livro com sinopse, reserva exemplares disponíveis e acompanha "Minhas reservas".

### 📖 Bibliotecário(a)
Cadastra livros (define quantos exemplares iniciais), cadastra alunos, registra empréstimos escolhendo o exemplar exato, devolve, renova, confirma reservas pendentes.

### 👨‍💼 Admin (Diretor)
Tudo do bibliotecário + cria/desativa contas da equipe + proteção contra desativação do último admin do sistema.

---

## 🛡️ Segurança

Implementação seguindo princípios OWASP:

- 🔐 **HTTPS automático** via Caddy + Let's Encrypt (renovação automática)
- 🎫 **JWT + Refresh token rotativo** com detecção de reuso (sinal de roubo)
- 🚫 **Lockout** após N tentativas + rate limiting por IP
- ⚖️ **CPF validado** com dígito verificador + rejeita sequências triviais (`11111111111`)
- ⏱️ **Anti-timing attack** no login do aluno (BCrypt dummy quando CPF não existe — não dá pra enumerar CPFs medindo tempo)
- 🎭 **Mascaramento LGPD** de CPF em listas vistas por terceiros (`123.***.***-01`)
- 🧨 **Sanitização XSS** nos campos de nome + allowlist de hosts para URLs de capa
- 🔒 **BCrypt strength 12** pra senhas, sem MD5/SHA1 em lugar algum
- 🚧 **Migrations versionadas** (Flyway) — nenhuma migration aplicada é editada
- 🚪 **Logout invalida access tokens** imediatamente (não espera expirar)

**Reportar vulnerabilidade:** use [GitHub Security Advisories](https://github.com/AlissonSouto7/AcervoLiber/security/advisories/new) (privado).

---

## 💰 Custo zero — só free tier

O sistema roda 100% em camadas gratuitas. **Custo mensal: R$ 0,00.**

| Serviço             | Camada usada                       | Limites               |
|---------------------|------------------------------------|-----------------------|
| **GCP VM**          | e2-micro Always Free               | 1 vCPU, 1 GB RAM, 30 GB disco — sem expiração |
| **Neon Postgres**   | Free Tier                          | 0.5 GB storage, autoscale to zero |
| **DuckDNS**         | Free                               | DNS dinâmico ilimitado |
| **Let's Encrypt**   | Free (via Caddy)                   | Certificados TLS renovados auto |
| **GitHub Actions**  | Free pra repos públicos            | Ilimitado |
| **GHCR**            | Free pra packages públicos         | Ilimitado |

⚠️ Nenhum serviço precisa de cartão de crédito permanente. GCP pede cartão pra criar conta mas a e2-micro Always Free não cobra dentro dos limites. **Caso atinja qualquer limite, o sistema simplesmente para — não vira cobrança automática.**

---

## 🏗️ Stack

| Camada       | Tech                                                                    |
|--------------|-------------------------------------------------------------------------|
| **Backend**  | Spring Boot 4.0.6 · Java 17 · JPA/Hibernate · Flyway · Spring Security |
| **Banco**    | PostgreSQL 16 (Neon em prod)                                            |
| **Frontend** | React 18 + Vite + TypeScript + Ant Design + TanStack Query + Zustand   |
| **Infra**    | Docker · Docker Compose · Caddy 2 (TLS automático)                      |
| **Hospedagem** | GCP e2-micro + Neon + DuckDNS                                         |
| **CI/CD**    | GitHub Actions (build amd64 + push GHCR + deploy SSH na VM)             |

---

## 🚀 Rodando local (desenvolvimento)

Pré-requisitos: Docker Desktop + Docker Compose v2.

```bash
# Clonar
git clone https://github.com/AlissonSouto7/AcervoLiber.git
cd AcervoLiber

# Volume do banco (uma vez)
docker volume create liber-pg-data

# Copiar template e ajustar valores
cp .env.example .env

# Subir o stack
docker compose up -d --build

# Acompanhar logs
docker compose logs -f app
```

**Acessos locais:**
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api/v1
- Swagger UI (só dev): http://localhost:8080/swagger-ui/index.html

**Primeiro acesso:** se `ADMIN_PASSWORD` no `.env` ficar vazio, o `AdminSeeder` gera uma senha aleatória e loga uma única vez no startup:

```bash
docker compose logs app 2>&1 | grep -A4 "ADMIN SEEDER"
```

---

## 🌐 Deploy em produção

Veja [`docs/RUNBOOK.md`](docs/RUNBOOK.md) para o procedimento completo:

1. **VM GCP e2-micro** (Always Free) — provisionamento + Docker + swap de 2 GB
2. **Neon Postgres** — projeto gratuito + connection string no `.env.prod`
3. **DuckDNS** — subdomínio gratuito apontando pro IP da VM
4. **Caddy** — TLS automático via Let's Encrypt (zero configuração de cert)
5. **GitHub Secrets** — `GCP_SSH_HOST`, `GCP_SSH_USER`, `GCP_SSH_PRIVATE_KEY`, `GCP_DEPLOY_PATH`
6. **CI/CD** — `git push origin main` → build amd64 → push GHCR → SSH → deploy (~5 min)

---

## 📂 Estrutura

```
acervo-liber/
├── src/                          backend Spring Boot
│   ├── main/
│   │   ├── java/com/liber/
│   │   │   ├── config/           SecurityConfig, properties, AdminSeeder
│   │   │   ├── controller/       REST endpoints
│   │   │   ├── service/          regras de negócio
│   │   │   ├── repository/       JPA repositories
│   │   │   ├── entity/           Livro, Exemplar, Aluno, Empréstimo, Reserva, ...
│   │   │   ├── dto/              records de request/response
│   │   │   ├── security/         JwtService, filtros, lockout
│   │   │   ├── util/             Cpf (validação + máscara), Isbn
│   │   │   └── exception/        GlobalExceptionHandler, exceptions de domínio
│   │   └── resources/
│   │       ├── application*.properties
│   │       └── db/migration/     migrations Flyway (V1..V18)
│   └── test/                     testes unitários + ITs (Testcontainers)
├── frontend/                     SPA React + Vite + TypeScript
│   ├── src/
│   │   ├── pages/                Login, Dashboard, Livros, Alunos, Empréstimos, etc.
│   │   ├── api/                  clientes axios + TanStack Query
│   │   ├── auth/                 authStore (Zustand)
│   │   ├── components/           AppLayout, ProtectedRoute, ErrorBoundary, ...
│   │   └── utils/                cpf (máscara), formatarData
│   ├── nginx.conf                proxy /api → app:8080
│   └── Dockerfile
├── docker-compose.yml            stack base (dev local com Postgres local)
├── docker-compose.gcp.yml        produção GCP (sem Postgres, usa Neon)
├── Caddyfile                     reverse proxy + TLS automático
├── scripts/
│   ├── backup.sh                 pg_dump diário rotacionando 30d
│   ├── gerar-pdf-manual.py       Markdown → PDF via ReportLab
│   └── seed-teste*.sql           cleanup/seed manual (referência)
├── .github/workflows/
│   ├── ci.yml                    build + tests em PR e push
│   └── deploy-gcp.yml            deploy automático em push pra main
└── docs/
    ├── RUNBOOK.md                operação em produção
    ├── MANUAL-PROFESSOR.md       manual do bibliotecário (fonte)
    └── MANUAL-PROFESSOR.pdf      manual em PDF entregue à escola
```

---

## 🌿 Branches

- **`main`** — código em produção (CI/CD dispara deploy automático a cada push)
- **`alisson`** — branch pessoal de trabalho

---

## 🧪 Testes

```bash
# Backend (unitários + Cpf util + serviços com mock)
./mvnw test

# Frontend (typecheck + bundle)
cd frontend && npm run build
```

**Cobertura atual:**
- ✅ Testes unitários de Services (Aluno, Refresh, LoginAttempt, RateLimit, Jwt, etc.)
- ✅ Testes do utilitário `Cpf` (validação de dígito verificador, máscara, normalização)
- ⏳ Testes pra Livro/Exemplar/Emprestimo flow (reescrita após refactor de exemplares — em andamento)

---

## 📋 Roadmap

### ✅ Feito
- CRUD completo: Livros, Exemplares, Alunos, Empréstimos, Reservas
- Portal do Aluno (catálogo + reservas + sinopse + detalhes)
- Renovação, edição e cancelamento de empréstimo
- Bloqueio por atraso, override no balcão pelo staff
- Mascaramento LGPD em listagens administrativas
- HTTPS + CI/CD + free tier
- Manual em PDF
- Migration V1 → V18 (estável)
- Easter egg 🥐 → 🐱 no login (homenagem)

### 🟡 Próximo (P1)
- Gestão avulsa de exemplares na UI (backend pronto, falta o drawer)
- Relatórios CSV (atrasados, inventário, devedores)
- Notificação ao aluno (badge in-app quando reserva confirmada)
- Testes `@WebMvcTest` nos controllers
- ESLint + Prettier no frontend

### 🔵 Futuro (P2)
- Gestão de turmas como entidade (com promoção fim de ano)
- Busca avançada de livros (filtro gênero, "só disponíveis")
- Acessibilidade (WCAG)
- Observabilidade (métricas de negócio + tracing)

---

## 🤝 Contribuindo

Pull Requests welcome. Algumas convenções:

1. **Branch a partir de `main`** com prefixo descritivo (`feat/`, `fix/`, `chore/`, `docs/`)
2. **Migration Flyway**: nunca edite migration já aplicada — sempre crie nova versão
3. **Commits** seguem [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `chore:`, `docs:`)
4. **Testes obrigatórios** em código novo de Service
5. **PII**: nunca exponha CPF/nome de aluno completo em listagens admin sem mascarar

---

## 📜 Licença

Projeto educacional/comunitário. Use à vontade, atribua quando possível.

---

## 💌 Créditos

Sistema concebido e desenvolvido por **Alisson Pinheiro Souto** ([@AlissonSouto7](https://github.com/AlissonSouto7)) para a Escola Municipal Gabriel José Pereira (Eunápolis-BA, 2026), no âmbito do Projeto de Extensão I da Engenharia de Software.

**Numidia** 🐱

> *"Quando há vontade real, a tecnologia bem aplicada cabe no bolso de qualquer escola."*
