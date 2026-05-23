# AcervoLiber

Sistema de gestão de biblioteca escolar — cadastro de livros e alunos, controle de empréstimos e devoluções, reservas via portal do aluno, e trilha de auditoria.

Desenvolvido para a escola Gabriel José Pereira.

## Stack

- **Backend:** Spring Boot 4.0.6 · Java 17 · JPA/Hibernate · Flyway · Spring Security (JWT) · jjwt 0.12.6
- **Banco:** PostgreSQL 16
- **Frontend:** React 18 + Vite + TypeScript + Ant Design + TanStack Query + Zustand
- **Infra:** Docker + Docker Compose · Caddy (TLS automático em prod)

## Rodando local (dev)

Pré-requisitos: Docker Desktop + Docker Compose v2.

```bash
# 1. Cria o volume do banco (uma vez)
docker volume create liber-pg-data

# 2. Copia o template de variaveis e ajusta valores
cp .env.example .env

# 3. Sobe o stack inteiro (Postgres + backend + frontend)
docker compose up -d --build

# 4. Acompanha logs
docker compose logs -f app
```

Acessos:
- **Frontend:** http://localhost:3000
- **Backend API:** http://localhost:8080/api/v1
- **Swagger UI (apenas dev):** http://localhost:8080/swagger-ui/index.html
- **Healthcheck:** http://localhost:8080/actuator/health

## Credenciais de primeiro acesso

No primeiro boot, o `AdminSeeder` cria o usuário admin a partir das variáveis `ADMIN_EMAIL` / `ADMIN_PASSWORD` do `.env`. Se `ADMIN_PASSWORD` estiver vazio, o seeder **gera uma senha aleatória forte e loga uma única vez** no startup (`grep "Senha do admin" nos logs`).

Em dev, com `SEED_DADOS_EXEMPLO=true` no `.env`, o `DadosExemploSeeder` popula livros/alunos/empréstimos de teste. As credenciais de teste estão documentadas em `seed-teste.sh`.

## Variáveis de ambiente principais

| Variável | Obrigatória? | Default | Descrição |
|---|---|---|---|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` | sim | `postgres` / `postgres` / `liber` | Credenciais do banco |
| `JWT_SECRET` | **sim em prod** | string dev (falha startup em prod) | Gerar com `openssl rand -base64 48` |
| `JWT_EXPIRATION_MS` | não | `900000` (15min) | Validade do access token |
| `JWT_REFRESH_EXPIRATION_MS` | não | `604800000` (7d) | Validade do refresh token |
| `CORS_ALLOWED_ORIGINS` | sim em prod | `localhost:*` | Lista de origens separadas por vírgula |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | sim | — | Admin inicial (senha vazia = gerada aleatória) |
| `SPRING_PROFILES_ACTIVE` | recomendado em prod | `dev` | Usar `prod` em produção |
| `SEED_DADOS_EXEMPLO` | não | `false` | Popular dados de teste no boot |
| `GOOGLE_BOOKS_API_KEY` | não | vazio | Acelera resolução de capas |

Lista completa em `.env.example` e `.env.prod.example`.

## Estrutura do projeto

```
liber/
├── src/                          backend Spring Boot
│   ├── main/
│   │   ├── java/com/liber/
│   │   │   ├── config/           SecurityConfig, OpenApiConfig, etc.
│   │   │   ├── controller/       endpoints REST
│   │   │   ├── service/          regras de negócio
│   │   │   ├── repository/       JPA repositories
│   │   │   ├── entity/           entidades JPA
│   │   │   ├── dto/              records de request/response
│   │   │   ├── security/         JwtService, filtros
│   │   │   └── seeder/           AdminSeeder, DadosExemploSeeder
│   │   └── resources/
│   │       ├── application*.properties
│   │       └── db/migration/     migrations Flyway V1__*..V13__*
│   └── test/                     unitários + integração (Testcontainers)
├── frontend/                     SPA React+Vite
│   ├── src/
│   │   ├── pages/                LoginPage, DashboardPage, LivrosPage, etc.
│   │   ├── api/                  clientes axios + queries TanStack
│   │   ├── auth/                 authStore (Zustand)
│   │   └── components/           AppLayout, ProtectedRoute, etc.
│   ├── nginx.conf                proxy /api → app:8080
│   └── Dockerfile
├── docker-compose.yml            stack base (dev/prod)
├── docker-compose.prod.yml       overlay produção (Caddy + backup)
├── Caddyfile                     reverse proxy + TLS automático
├── scripts/backup.sh             pg_dump diário rotacionando 30d
├── .github/workflows/            CI + Deploy
└── docs/RUNBOOK.md               operação em produção
```

## Branches

- `main` — código em produção. Protegida (PR + CI verde obrigatório).
- `dev` — integração. Recebe PRs das branches de feature.
- `alisson` — branch pessoal de trabalho (deriva de `dev`).

Fluxo: `alisson` → PR para `dev` → após aprovação, `dev` → PR para `main` → deploy automático.

## Testes

```bash
# Unitarios (rapidos, sem Docker)
./mvnw test

# Inclui testes de integracao (Testcontainers — precisa Docker rodando)
./mvnw verify

# Frontend
cd frontend && npm run build   # build (typecheck + bundle)
```

## Operação em produção

Veja [`docs/RUNBOOK.md`](docs/RUNBOOK.md): deploy, backup/restore, criar bibliotecário, rotacionar `JWT_SECRET`, destravar conta admin, troubleshooting.

## Segurança

Auditoria profunda em `AUDIT_DEEP_SECURITY.md` — 7 fases, 269 achados, 31 fixes aplicados ao vivo. Pendências priorizadas em `AUDIT_PENDENCIAS.md`.

**Reportar vulnerabilidade:** abra uma issue privada (security advisory) ao invés de issue pública.
