# Audit de Pendências — O que falta fazer — AcervoLiber

> O que falta para o sistema ir para produção na escola Gabriel José Pereira.
> Data: 2026-05-22. Prioridades: P0 bloqueia produção · P1 importante · P2 desejável · P3 nice-to-have.

> **Nota:** o `AUDIT.md` está honesto mas otimista/desatualizado — cita Flyway V7/51 testes, mas o schema já está em **V9**.

---

## P0 — Bloqueiam produção

### Projeto não é um repositório git → CI nunca rodou
`.github/workflows/ci.yml` existe, mas o projeto **não está versionado**. Logo, o CI nunca rodou e os testes de integração (`AdminFlowIT`, `EmprestimoFlowIT`, `LiberApplicationIT`) **nunca foram executados** — os fluxos ponta a ponta estão não verificados. É o primeiro passo a destravar. Esforço: pequeno (git init + remote + push).

### Sem terminação HTTPS/TLS
`frontend/nginx.conf` / infra. O `SecurityConfig` envia HSTS e o Nginx tem headers, mas **nada termina TLS** — login e JWT trafegariam em HTTP puro. Inaceitável com dados de menores. Falta certificado (Let's Encrypt) + `listen 443 ssl`, ou proxy reverso na borda. Esforço: médio.

### Sem estratégia de backup do banco
Não há `pg_dump` agendado, nem volume de backup separado, nem runbook de restore. Perder o volume `liber-pg-data` = perder todo o acervo e histórico. Esforço: pequeno/médio.

---

## P1 — Importantes antes de entregar

### Funcionalidades de biblioteca ausentes

- **Renovação de empréstimo** — não existe endpoint nem método. É a operação mais comum depois de emprestar/devolver. Hoje só dá pra devolver e re-emprestar (perde histórico). Esforço: médio.
- **Multas / penalidades / bloqueio por atraso** — o sistema calcula o semáforo de urgência mas **não há nenhuma consequência por atraso**. Um aluno com livro atrasado há meses continua emprestando normalmente. A escola provavelmente precisa de pelo menos "aluno com atraso não empresta". Esforço: médio.
- **Edição/cancelamento de empréstimo** — só existe `POST` registrar e `POST /{id}/devolucao`. Não há como corrigir um empréstimo lançado errado. Esforço: pequeno/médio.

### Testes
- **Cobertura incompleta** — sem teste unitário: `DashboardService`, `AlunoService`, `AuthService`, `UsuarioService`, `CapaService`, `AdminSeeder`, `SenhaForteValidator`, e **todos os 9 controllers** (nenhum `@WebMvcTest`). `AuthService`/`UsuarioService` sem teste é arriscado por serem segurança. Esforço: médio.

### Documentação
- **Sem README na raiz** — só o `HELP.md` boilerplate do Spring Initializr. Falta: o que é o sistema, como subir, variáveis obrigatórias, credenciais de primeiro acesso. Esforço: pequeno.
- **Sem runbook de operação** — como fazer backup/restore, criar bibliotecário, rotacionar `JWT_SECRET`, destravar conta admin. Essencial para entregar a uma escola sem TI dedicada. Esforço: médio.

### CI/CD incompleto
`ci.yml` só faz `mvnw verify` + `npm run build`. Falta: **lint do frontend** (não há ESLint configurado), gate de cobertura, build/publicação da imagem Docker, estágio de deploy. Esforço: médio.

### Edge case
- `EmprestimoService:120-124` — quando `incrementarEstoque` na devolução afeta 0 linhas, o código só faz `log.warn` e segue; o estoque fica errado sem ninguém ser alertado de forma acionável. Deveria gerar registro de auditoria/alerta. Esforço: pequeno.

---

## P2 — Desejáveis

- **Gestão de turmas / ano letivo** — `turma` é string livre no `Aluno`; não há entidade Turma nem promoção/arquivamento no fim do ano. A base acumula alunos antigos indefinidamente. Esforço: grande.
- **Relatórios / exportação** — não há exportação CSV/PDF de nada (lista de devedores, relatório por turma, inventário do acervo). Bibliotecário de escola tipicamente precisa imprimir a lista de atrasados. Esforço: médio.
- **JaCoCo / medição de cobertura** — nenhum plugin configurado; não dá pra impor mínimo no CI. Esforço: pequeno.
- **Testes de frontend** — zero (sem Vitest/Jest/Testing Library). O interceptor de refresh e o `authStore` não têm cobertura. Esforço: médio.
- **Error Boundary no frontend** — `App.tsx` sem nenhum; qualquer erro de render derruba a SPA pra tela branca. Esforço: pequeno.
- **Observabilidade** — há probes e `/actuator/metrics`, mas nenhuma métrica de negócio, sem tracing, sem alertas, logs JSON sem destino configurado. Esforço: médio.
- **Reserva confirmada não revalida limite do aluno** — `ReservaService.confirmar`. Esforço: pequeno.
- **Rate limit / lockout só in-memory** — funciona em instância única (provavelmente OK para uma escola), mas é um limite a documentar conscientemente. Esforço: médio (se escalar).
- **Senha default fraca no compose** — `ADMIN_PASSWORD: "${ADMIN_PASSWORD:-@Admin2026}"`; o profile `prod` deveria recusar subir com o default. Esforço: pequeno.

---

## P3 — Nice-to-have

- **Notificação ao aluno** — reserva confirmada/recusada e atraso são "mudos"; o aluno só descobre entrando em "Minhas reservas". Esforço: médio (e-mail) / pequeno (badge in-app).
- **Busca avançada** — busca de livros é um único `termo` com `LIKE`; sem filtro por gênero, "só disponíveis" ou ordenação. Esforço: pequeno/médio.
- **Acessibilidade** — não verificada (contraste, teclado, `aria-*`). Esforço: médio.
- **Validações de domínio no frontend** — formato de ISBN/matrícula só validado no backend. Esforço: pequeno.
- **i18n** — textos em PT-BR hard-coded; aceitável para uma escola brasileira. Esforço: grande (se algum dia precisar).
- **`JAVA_OPTS`** — definido no Dockerfile mas não ajustável via compose. Esforço: pequeno.

---

## Resumo — destravar produção

| Ordem | Item | Prioridade |
|---|---|---|
| 1 | Versionar em git + remote (CI passa a rodar e validar os `*IT`) | P0 |
| 2 | Terminação HTTPS/TLS | P0 |
| 3 | Backup do banco automatizado + runbook de restore | P0 |
| 4 | Renovação, bloqueio por atraso, edição de empréstimo | P1 |
| 5 | Testes de `AuthService`/`UsuarioService` + controllers | P1 |
| 6 | README + runbook de operação | P1 |
| 7 | Endurecer o pipeline (lint, build de imagem, deploy) | P1 |

As lacunas mais sérias não são de arquitetura (o backend é bem feito em segurança e concorrência) — são **operacionais** (HTTPS, backup, CI nunca executado) e de **completude funcional de biblioteca** (renovação, atraso, relatórios).
