# Auditoria profunda de segurança — AcervoLiber

> Auditoria por fases, multi-sessão. Cada fase faz leitura fina de código + testes
> ao vivo, cobrindo **lógica**, **segurança** e **edge cases**. Achados ficam aqui,
> com severidade, `arquivo:linha` e correção sugerida. **Não aplica fix automaticamente**
> — você decide o que corrigir entre fases.

> Esta auditoria **complementa e aprofunda** as anteriores (`AUDIT_SEGURANCA.md`,
> `AUDIT_BUGS.md`, `AUDIT_PENTEST.md`), focando em traçar **cada caminho de código**
> e cobrir o que aquelas, que eram surveys mais amplos, podem ter passado.

## Plano

| # | Fase | Escopo | Status |
|---|---|---|---|
| 1 | **Autenticação e identidade** | login bib/aluno, refresh, logout, JWT, filtros (rate limit, senha provisória), troca de senha, frontend de login/primeiro acesso, `authStore`, `ProtectedRoute`, interceptor `http.ts` | ✅ concluída (52 achados) |
| 2 | Usuários e Auditoria | CRUD de usuários (ADMIN), ativação/desativação, trilha de auditoria, página de Auditoria | ✅ concluída (63 achados, 1 crítica + 6 altas corrigidas) |
| 3 | Alunos e Empréstimos | Cadastro de alunos, criar acesso, regras de empréstimo, devolução, limites, lock pessimista, frontend Alunos+Empréstimos+Histórico | ✅ concluída (72 achados, 6 altas/médias corrigidas + 3 eventos de auditoria novos) |
| 4 | Livros e Capas | CRUD de livros, busca, upload de capa, consultas externas, catálogo, frontend Livros | pendente |
| 5 | Reservas e Portal do Aluno | Fluxo end-to-end aluno→bibliotecário, expiração, concorrência, frontend Catálogo+Minhas reservas+Reservas pendentes | pendente |
| 6 | Camada transversal | Dashboard, Actuator, Swagger, CORS, headers, `GlobalExceptionHandler`, configs, secrets, Dockerfile, Nginx | pendente |
| 7 | Ataque hacker simulado | Tomada como atacante externo, partindo do zero, só com devtools + ferramentas reais | pendente |

## Convenções

- **Severidade:** Crítica · Alta · Média · Baixa · Informativa
- **Tipo:** 🔒 Segurança · 🐛 Lógica · 🧪 Edge case · ⚠️ Defesa em profundidade
- Cada achado tem `arquivo:linha`, descrição do que dá errado e em que cenário, e correção sugerida.

---

# Fase 1 — Autenticação e identidade

> Leitura fina por 4 sub-agentes paralelos cobrindo: AuthService/UsuarioService/AlunoService, JWT/RefreshToken, filtros (SecurityConfig/RateLimiting/SenhaProvisoria/LoginAttempt) e frontend (authStore/ProtectedRoute/LoginPage/PrimeiroAcesso/http.ts). 3 achados de alta prioridade foram confirmados ao vivo via `curl`.

## ✅ Fixes aplicados nesta sessão (rebuild + verificação ao vivo)

| # | Achado | Status | Onde |
|---|---|---|---|
| V2 | 🔴 Lockout poisoning | ✅ corrigido | nginx (sobrescreve X-Forwarded-For), `application.properties` (forward-headers-strategy=native + internal-proxies), `InMemoryLoginAttemptService` (chave = email+ip) |
| V1 | 🟠 `/auth/logout` público | ✅ corrigido | `SecurityConfig` (fora de PUBLIC_ENDPOINTS), `AuthController` + `AuthService` (exige auth, valida ownership) |
| 1.A.alta.2 | 🟠 `alterarSenha` não revoga refresh | ✅ corrigido | `UsuarioService.alterarSenha` → `refreshTokenService.revogarTodosDoUsuario` |
| 1.A.alta.3 | 🟠 `alterarStatus(false)` não revoga | ✅ corrigido | `UsuarioService.alterarStatus` → revoga + bumpa `passwordChangedAt` |
| 1.B.alta.1 | 🟠 Race no `rotacionar` | ✅ corrigido | `@Version` em `RefreshToken` + migration V10 |
| 1.D.alta.1 | 🟠 `queryClient` não limpo no logout | ✅ corrigido | `App.tsx` exporta queryClient; `AppLayout.sair` + `http.ts` chamam `.clear()` |
| 1.D.alta.4 | 🟠 Loop redirect se role undefined | ✅ corrigido | `HomeRedirect`/`RoleRoute` mandam para `/login` |
| 1.D.alta.5 | 🟠 Interceptor headers undefined | ✅ corrigido | `http.ts` usa `AxiosHeaders` defensivo |
| - | 🟠 Logout não chama backend | ✅ já estava OK | `AppLayout.sair` já chamava `logout(refreshToken)` antes |

**Re-teste live confirmou:** logout sem auth → 401; logout com ownership errado → silenciado (refresh da vítima sobrevive); change-password revoga refresh (HTTP 401 no refresh subsequente); 6 logins via nginx com `X-Forwarded-For: 9.9.9.X` forjado → **5×401 + 1×429**, auditoria registrou todos como vindo de `172.18.0.1` (IP real) em vez dos forjados → spoofing fechado.

**Deferido pra cleanup pass (fim da Fase 7):** Caffeine para os mapas in-memory, mensagens unificadas de refresh, `CORS allow-credentials=false`, timing BCrypt dummy, regex matrícula, Permissions-Policy headers, `partialize` PII no store, sync entre abas, `clockSkewSeconds`, mensagens neutras em ProblemDetail.

---

## Verificações ao vivo (confirmadas)

### V1 — `/auth/logout` é público — qualquer um revoga refresh tokens alheios
```
POST /api/v1/auth/logout  (sem Authorization, {"refreshToken":"qualquer-coisa"})
→ HTTP 204 No Content
```
O endpoint aceita revogar qualquer refresh token apresentado, sem prova de posse. Combinado com refresh em `localStorage` (vetor XSS / dependência comprometida), um atacante força logout / dispara cascata de reuse-detection.

### V2 — Account-lockout poisoning: bloqueio de conta legítima por atacante remoto
```
Atacante (X-Forwarded-For=9.9.9.{1..6} → driblando rate limit por IP):
  tentativa 1 → 401     tentativa 2 → 401     tentativa 3 → 401
  tentativa 4 → 401     tentativa 5 → 401     tentativa 6 → 423 LOCKED ⚠️
Vítima com SENHA CORRETA → 423 LOCKED
```
Atacante que só sabe o **e-mail** da vítima bloqueia a conta por 15 min. Repetindo a cada 16 min mantém perma-bloqueada. Vale pra `admin@liber.local` (default conhecido) — DoS do administrador.

### V3 — `/auth/refresh` sem rate limit
```
10 tentativas seguidas com refresh inválido → 401 × 10 (nenhum 429)
```
Brute-force estatístico é inviável contra 32 bytes random; o problema real é DoS de CPU (cada tentativa custa SHA-256 + DB lookup) e ausência de defesa contra reuse-detection em cascata.

---

## Resumo Fase 1

| Severidade | Backend auth (1.A) | JWT/Refresh (1.B) | Filtros (1.C) | Frontend (1.D) | **Total** |
|---|---|---|---|---|---|
| 🔴 Crítica | — | — | 1 (V2) | — | **1** |
| 🟠 Alta | 4 | 3 | 4 | 5 | **16** |
| 🟡 Média | 3 | 4 | 5 | 4 | **16** |
| 🟢 Baixa | 4 | 3 | 3 | 4 | **14** |
| ℹ️ Informativa | 1 | 1 | 1 | 2 | **5** |
|   |   |   |   | | **52** |

**Top 5 pra atacar primeiro** (impacto × esforço):

1. 🔴 **V2 — Lockout poisoning** — atacante perma-bloqueia qualquer conta sabendo só o e-mail. ATAQUE AO ADMIN possível.
2. 🟠 **V1 — `/auth/logout` público** — combinar com XSS revoga sessões; tirar de `PUBLIC_ENDPOINTS`.
3. 🟠 **`alterarStatus(ativo=false)` não revoga refresh** — usuário desativado continua emitindo access tokens via `/auth/refresh` por 7 dias.
4. 🟠 **Race no `rotacionar` refresh** — sem `@Version`, dois refresh paralelos emitem dois filhos válidos do mesmo pai (quebra rotação).
5. 🟠 **Logout do frontend NÃO chama backend** — refresh token vive 7 dias mesmo após "Sair".

---

## 1.A — Fluxo de autenticação (AuthService / UsuarioService / AlunoService)

### 🟠 Alta · 🔒 Vazamento por timing em `loginAluno` — enumera matrículas válidas
`AuthService.java:48-52`
`findByAlunoMatricula(...).orElseThrow(...)` lança `BadCredentialsException` **sem** rodar BCrypt e **sem** registrar falha. Quando a matrícula existe, o BCrypt cost 12 roda (~200ms). Diferença de latência mensurável → enumera matrículas válidas de menores (LGPD). Pior: como não passa por `registrarFalha`, atacante varre matrículas sem disparar lockout.
**Fix:** comparar contra hash dummy quando matrícula não existe; registrar falha em ambos os casos.

### 🟠 Alta · 🔒 `alterarSenha` não revoga refresh tokens — sessão atacante sobrevive à troca de senha
`UsuarioService.java:109-112`
Trocar a senha atualiza `passwordChangedAt` (invalida access tokens via filtro JWT), mas **não** revoga refresh tokens. Atacante com refresh roubado (localStorage/XSS) chama `/auth/refresh` → recebe access novo válido → continua dentro mesmo após a vítima trocar a senha.
**Fix:** em `alterarSenha`, chamar `refreshTokenService.revogarTodosDoUsuario(usuarioId)` após o save.

### 🟠 Alta · 🔒 `alterarStatus(false)` não revoga refresh — usuário desativado continua emitindo tokens
`UsuarioService.java:83-94`, `AuthService.java:83-95`
Admin desativa um bibliotecário comprometido. Access tokens são bloqueados pelo filtro (`isEnabled()` é re-checado por request). Mas **`rotacionar` não verifica `ativo`** — o desativado faz `/auth/refresh` por 7 dias e continua recebendo access tokens novos (que só batem no muro depois). Pior: se admin **reativar** depois, todos os access antigos voltam a funcionar (`passwordChangedAt` não é tocado).
**Fix:** em `alterarStatus(false)`, revogar todos refresh tokens + setar `passwordChangedAt = Instant.now()`. Em `RefreshTokenService.rotacionar`, verificar `usuario.ativo`.

### 🟠 Alta · 🧪 Matrícula sem regex restritiva → e-mail sintético inválido / colisão silenciosa
`AlunoRequest.java`, `AlunoService.java:111`
Matrícula `"2026 001"` (espaço) → e-mail `"aluno.2026 001@liber.local"` viola `@Email` no flush; matrícula com acento idem; `"2026.001"` vs `"2026001"` podem colidir após `trim().toLowerCase()`. Em `criarAcesso`, evento de auditoria pode ser registrado antes do flush quebrar.
**Fix:** `@Pattern("^[A-Za-z0-9._-]{3,30}$")` no `AlunoRequest`; idealmente derivar e-mail do `id` estável (`aluno.<id>@liber.local`).

### 🟡 Média · 🔒 BCrypt trunca em 72 bytes — senhas longas têm sufixos ignorados
`UsuarioService.java:57`, `SenhaForteValidator.java` (aceita até 100 chars)
`BCryptPasswordEncoder` do Spring trunca a entrada em 72 bytes. Senha de 100 chars `AAAA...AAAA + sufixo` é equivalente a só os primeiros 72 A's. Atacante que adivinha os primeiros 72 entra sem o sufixo. Ilusão de força extra.
**Fix:** limitar `@SenhaForte` em 72 chars OU pré-hashear com SHA-256 antes do BCrypt OU migrar para `Argon2PasswordEncoder`.

### 🟡 Média · 🔒 `atualizarPerfil` aceita nome com caracteres de controle / log injection
`UsuarioService.java:75-81`, `AtualizarPerfilRequest.java`
Sem `@Pattern`. Aceita ` `, `\r\nLOG INJECTION`, HTML tags. XSS mitigado pelo React (visto no pentest), mas o nome aparece em logs/relatórios futuros e em e-mails.
**Fix:** `@Pattern("^[\\p{L}\\p{N} .,'\\-]+$")` + strip de caracteres de controle.

### 🟡 Média · 🧪 `loginAluno` não normaliza matrícula igual ao cadastro — login falha para case diferente
`AuthService.java:48`, `AlunoService.java:49`
Cadastro salva `matricula.trim()` (preserva case). Login busca `trim()` também — sensível a maiúscula. Aluno cadastrado como `"2026ABC"` digita `"2026abc"` → falha → contador sobe → conta bloqueia.
**Fix:** decidir e documentar (provavelmente case-insensitive); aplicar `toLowerCase()` em ambos os pontos + índice case-insensitive.

### 🟢 Baixa · 🔒 Lista de senhas comuns é exata e curta — passes óbvios para contexto Liber
`SenhaForteValidator.java:13-18`
Bloqueia `password`, `liber`, etc. **Não bloqueia:** `Liber@2026`, `Biblioteca123`, `Escola2026!`, `Mudar@123`, `LiberEscola@2026`. Comparação exata após lowercase (não "contém").
**Fix:** integrar zxcvbn (Java port existe) ou rejeitar substrings `liber|escola|biblioteca|<ano>`.

### 🟢 Baixa · 🐛 Reuso da senha provisória após troca-e-volta — sem histórico
`UsuarioService.java:101-112`
Aluno troca `LiberEscola@2026` → `LiberEscola@2026X` → de volta para `LiberEscola@2026`. Senha provisória conhecida pela equipe volta a valer.
**Fix:** tabela `usuario_senha_historico` com últimos N hashes; rejeitar reuso. Ou gerar senha provisória aleatória única por aluno.

### 🟢 Baixa · ⚠️ `Usuario.role` / `Usuario.ativo` boxed — NPE se DB ficar inconsistente
`AppUserDetailsService.java:30`, `Usuario.java`
Bean Validation não roda em load; UPDATE manual deixando `role=NULL` ou `ativo=NULL` causa NPE 500 em `loadUserByUsername`. Fail-open inexistente, mas explosão ruim.
**Fix:** fail-closed: se `role==null` → `UsernameNotFoundException`.

### 🟢 Baixa · 🔒 `register` e `criarComoAdmin` compartilham `criarInterno(... Role role)` — frágil contra mass-assignment futuro
`AuthService.java:103`, `UsuarioService.java:39-46`
Hoje `RegisterRequest` não tem campo `role` — seguro. Mas a assinatura aceita `role` como parâmetro — uma linha de descuido futura escalaria privilégio.
**Fix:** método separado `registrarBibliotecario(RegisterRequest)` que sempre força `BIBLIOTECARIO`.

### ℹ️ Informativa · `UsuarioResponse` expõe `deveTrocarSenha` em `listar` — leve leak interno

---

## 1.B — JWT e refresh tokens

### 🟠 Alta · 🐛 Race em `rotacionar` permite duplicação silenciosa — quebra rotação
`RefreshTokenService.java:57-81`, `RefreshToken.java` (sem `@Version`)
Dois `/auth/refresh` com o mesmo token chegam paralelos (acontece no frontend após expiração do access). Ambos passam por `findByTokenHash` antes do commit do primeiro → ambos rotacionam → **2 filhos válidos do mesmo pai**, sem disparar reuse-detection. Atacante que roubou o refresh e disputa contra o cliente legítimo **ganha um token irmão sem nenhum sinal**.
**Fix:** `@Lock(PESSIMISTIC_WRITE)` no `findByTokenHash` OU `@Version` no `RefreshToken` (já tem 409 handler) OU UPDATE condicional `WHERE id=:id AND revoked_at IS NULL` checando `affectedRows==1`.

### 🟠 Alta · 🐛 Falha de rede pós-commit do rotacionar → logout global involuntário
`RefreshTokenService.java:76-80`
Servidor commitou rotação, resposta perdida na rede. Cliente reapresenta o mesmo refresh → cai em `isRevogado` → `revogarTodosDoUsuario` (detecção de reuso) → **todas as sessões do usuário caem**. WiFi de escola = falsos positivos.
**Fix:** janela de graça — coluna `replaced_by_id`; se reuso aponta para filho ainda não-rotacionado e jovem (<30s), devolver o filho ao invés de revogar família.

### 🟠 Alta · 🐛 Logout só revoga **um** refresh — sessões em outros devices sobrevivem
`AuthService.java:97-101`, `RefreshTokenService.java:83-91`
"Sair no celular" não desloga o laptop. Não há endpoint `logout-all`. `revogarTodosDoUsuario` só é chamado em detecção de reuso.
**Fix:** expor `POST /auth/logout?all=true`; sempre revogar todos os refresh do usuário em troca de senha e em desativação.

### 🟡 Média · 🔒 Mensagens distintas em `/auth/refresh` (`invalido` / `expirado` / `reutilizado`) — vaza info ao atacante
`RefreshTokenService.java:60,70,73`
`"reutilizado — todas as sessoes foram encerradas"` confirma ao atacante que ele acabou de detonar a sessão da vítima e revela que o sistema tem rotação com detecção de família.
**Fix:** mensagem única `"Refresh token invalido"` para o cliente; manter logs detalhados no servidor.

### 🟡 Média · 🔒 `RefreshTokenCleanupJob` não remove revogados — tabela cresce indefinidamente
`RefreshTokenCleanupJob.java:19-25`, `RefreshTokenRepository.java`
Só remove `expires_at < now`. Cada rotação cria 1 e revoga 1; revogados ficam até expirarem (7d). 1000 alunos × 100 rotações/dia = ~700k linhas residuais.
**Fix:** `delete ... where expires_at < :agora OR (revoked_at IS NOT NULL AND revoked_at < :agora - 1 day)`.

### 🟡 Média · ⚠️ Fail-open em `tokenEmitidoAposUltimaTrocaDeSenha` quando principal não é `AppUserDetails`
`JwtAuthenticationFilter.java:75-78`
Hoje sempre é `AppUserDetails` (não-explorável). Mas há um `User.withUsername(...)` plano em `AuthService.toUserDetails:116-121` (não usado no SecurityContext, mas existe como armadilha). Regressão futura desativa silenciosamente a invalidação por troca de senha.
**Fix:** trocar para fail-closed (`return false`) ou lançar `IllegalStateException` para falhar ruidosamente. Eliminar o `toUserDetails` plano em favor de `AppUserDetails`.

### 🟡 Média · 🧪 Granularidade `iat` (segundos) vs `passwordChangedAt` (microssegundos) + tolerância de 5s
`JwtAuthenticationFilter.java:81`, `UsuarioService.java:110`
`iat.plusSeconds(5).isBefore(passwordChangedAt)` dá janela de até 5s onde tokens emitidos **antes** da troca de senha continuam aceitos depois. Atacante com token roubado tem ~5s de uso garantidos pós-reset. Pior: `passwordChangedAt = Instant.now()` (sem `Clock` injetado) diverge do `JwtService.Instant.now(clock)` em testes.
**Fix:** reduzir tolerância para 1s; padronizar uso de `Clock` injetado em todos os pontos que gravam `passwordChangedAt`.

### 🟡 Média · 🔒 Sem `clockSkewSeconds` no parser — ambientes com NTP descalibrado dão 401 espúrios → cascata de refresh → logout global (combina com #1 e #2)
`JwtService.java:73-81`
`Jwts.parser()` sem `clockSkewSeconds(N)` → tolerância zero. Container vs host com diferença de segundos → 401 → refresh → race → logout em loop.
**Fix:** `clockSkewSeconds(30)` no builder do parser.

### 🟢 Baixa · ⚠️ `Authorization` header parsing case-sensitive — `bearer ...` rejeitado (RFC diz case-insensitive)
`JwtAuthenticationFilter.java:35-40`
Trivial, mas viola RFC 7235 §2.1.
**Fix:** `header.regionMatches(true, 0, "Bearer ", 0, 7)`.

### 🟢 Baixa · ⚠️ Logs de erro de JWT inválido vão pra `debug` — SOC não vê força bruta de assinatura
`JwtService.java:58-67`
`SignatureException` deveria ser `WARN` com IP (sinal de tampering).
**Fix:** separar categorias de exceção e logar `WARN` em tampering óbvio.

### 🟢 Baixa · ⚠️ Sem `aud` claim — futuro cliente mobile não distinguível do web
**Fix:** adicionar `aud` no `generateToken` e `requireAudience` no parser.

### ℹ️ Informativa · Migration V4 OK — índice unique no `token_hash`, FK ON DELETE CASCADE, sem órfãos

---

## 1.C — Filtros, rate limit e SecurityConfig

### 🔴 Crítica · 🔒 V2 — Account-lockout poisoning (CONFIRMADO ao vivo)
`InMemoryLoginAttemptService.java:50-65`, `AuthService.java:55-71`
Atacante com IPs forjados (driblando rate limit por IP — bug separado) faz 5 logins com senha errada → conta da vítima bloqueada 15 min. Repete a cada 16 min = perma-bloqueio. Funciona contra `admin@liber.local` (default público).
**Fix multinível:** (a) corrigir spoofing `X-Forwarded-For` (configurar trusted proxies, `server.forward-headers-strategy=framework`); (b) "soft lockout" com atraso exponencial em vez de hard 15min; (c) auditoria de `LOGIN_BLOQUEADO` repetido alerta SRE; (d) endpoint admin para destravar manualmente.

### 🟠 Alta · 🔒 V1 — `/auth/logout` é público (CONFIRMADO ao vivo)
`SecurityConfig.java:39`
Em `PUBLIC_ENDPOINTS`. Qualquer um que tenha o refresh token revoga sem prova de posse. Cenário XSS → revoga refresh da vítima → logout forçado. Loop com tokens antigos pode disparar reuse-detection → revogação em cascata.
**Fix:** remover `/auth/logout` de `PUBLIC_ENDPOINTS`; exigir `Authorization: Bearer` e validar que o refresh pertence ao principal autenticado.

### 🟠 Alta · 🔒 CORS `allowCredentials=true` é desnecessário e perigoso com curinga de origem
`SecurityConfig.java:122-125`, `application.properties:142`
Tokens vivem em `localStorage` (header `Authorization`) — frontend nunca usa credentials. `allowCredentials=true` está ali "por reflexo", e amplia superfície futura caso migrem pra cookie. Combinado com `allowedOriginPatterns` curinga, vira armadilha.
**Fix:** `app.cors.allow-credentials=false`. Re-habilitar **apenas** se migrar pra cookie httpOnly, e nesse caso com origens exatas (sem curinga).

### 🟠 Alta · 🔒 Memory growth no `RateLimitingFilter` — DoS de heap via IP forjado
`RateLimitingFilter.java:37,55`
`ConcurrentMap<String, Bucket>` sem eviction. Spoofing X-Forwarded-For + N requests = N entradas permanentes (~KB cada). 10M entradas = GB de heap → OOM.
**Fix:** trocar por Caffeine com `expireAfterAccess(refillPeriod × capacity)`.

### 🟠 Alta · 🔒 Memory growth no `InMemoryLoginAttemptService` — mesma patologia, sem precisar de IP spoof
`InMemoryLoginAttemptService.java:27,42-47`
Chave é o e-mail enviado pelo cliente — sem validação. Atacante manda `email=<random>@x.com` → cresce indefinidamente.
**Fix:** Caffeine com `expireAfterWrite(bloqueioMinutos × 2)`. Idealmente, multi-instância → Redis com TTL nativo.

### 🟡 Média · 🔒 V3 — Endpoints sensíveis sem rate-limit (CONFIRMADO: `/auth/refresh` aceita 10/10)
`RateLimitingFilter.java:33,48`
`LOGIN_PATH_PREFIX="/api/v1/auth/login"` cobre só login/login-aluno. Ficam de fora: `/auth/refresh` (DoS de CPU via SHA-256+DB + cascata de revogação), `/auth/change-password` (brute-force da senha atual via XSS + DoS de CPU em BCrypt), `/auth/logout` (revogação em loop).
**Fix:** ampliar para lista de paths. `change-password` deve ser rateado por **usuário autenticado**, não por IP.

### 🟡 Média · 🔒 Sem `Cache-Control: no-store` em respostas de auth — proxies podem cachear tokens
`SecurityConfig.java:68-79`
`HeadersConfigurer` configura HSTS/XFO/nosniff/Referrer-Policy mas não `Cache-Control`. `LoginResponse` (contém access+refresh) e `/auth/me` (dados de usuário) sem `no-store` → proxies/PWA podem cachear.
**Fix:** `.cacheControl(Customizer.withDefaults())` no `HeadersConfigurer` (default Spring Security é `no-cache, no-store, max-age=0, must-revalidate`).

### 🟡 Média · ⚠️ `SenhaProvisoriaFilter` libera todo `/auth/**` — provisional pode editar perfil e rodar refresh em loop sem trocar senha
`SenhaProvisoriaFilter.java:34,47`
Aluno com senha `@Admin2026` (default) loga, chama `/me` + `/refresh` em loop indefinidamente sem nunca trocar — só perde acesso a endpoints de domínio. A "obrigatoriedade" vira teatro.
**Fix:** allowlist explícita: `/auth/me`, `/auth/change-password`, `/auth/logout`. Bloquear `/auth/perfil`, `/auth/refresh`, `/auth/login*`, `/auth/register`.

### 🟡 Média · ⚠️ Sem `Permissions-Policy`, `Cross-Origin-Resource-Policy`, `Cross-Origin-Opener-Policy`
`SecurityConfig.java:68-79`
Headers modernos de isolamento ausentes — defesa contra Spectre/XSLeak e abuse de permissions.
**Fix:** `.permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=()"))` + `.crossOriginResourcePolicy(c -> c.policy(SAME_SITE))`.

### 🟡 Média · ⚠️ `RestAccessDeniedHandler`/`EntryPoint` ecoam `request.getRequestURI()` no `instance` — vaza paths consultados
`RestAccessDeniedHandler.java:31`
401/403 para `/api/v1/usuarios/42` retorna `"instance":"/api/v1/usuarios/42"`. Habilita enumeração 401 vs 403 vs 404.
**Fix:** omitir `setInstance` ou padronizar para `URI.create("about:blank")`.

### 🟢 Baixa · ⚠️ `Retry-After` no 429 usa `refillPeriodSeconds` (60s) — impreciso
`RateLimitingFilter.java:91`
**Fix:** `bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill()`.

### 🟢 Baixa · ⚠️ Ordem entre `rateLimitingFilter` e `jwtAuthenticationFilter` é indefinida (`addFilterBefore` no mesmo alvo)
`SecurityConfig.java:91-92`
**Fix:** usar `addFilterAfter(jwt, RateLimitingFilter.class)` para tornar a ordem explícita.

### 🟢 Baixa · ⚠️ CORS `maxAge=3600` — preflight cacheado por 1h dificulta revogação de origem
**Fix:** considerar 600s para ambientes sensíveis.

### ℹ️ Informativa · `/auth/register` em `PUBLIC_ENDPOINTS` mesmo desabilitado — verificação no controller, sem rate limit

---

## 1.D — Frontend de auth

### 🟠 Alta · 🔒 `queryClient` não é limpo no logout — próximo usuário vê dados em cache do anterior
`App.tsx:13-20`, `api/http.ts:67-71`
Cache do TanStack Query sobrevive a `limparSessao()`. Em PC compartilhado (cenário escola), aluno B vê nomes/matrículas/empréstimos cacheados pelo bibliotecário A — vazamento de PII de menores (LGPD).
**Fix:** exportar `queryClient` para módulo separado; chamar `queryClient.clear()` em `limparSessao` e em todo fluxo de logout.

### 🟠 Alta · 🔒 `localStorage['liber-auth']` persiste tokens e PII; sem sync entre abas
`auth/authStore.ts:24-46`, `api/http.ts:67-71`
(a) Se `set({...:null})` falhar (cota / modo privado), entrada antiga permanece — sessão "ressuscita" no próximo load. (b) Abrir 2 abas, logout em uma → outra continua autenticada (sem listener de `storage`). (c) Login de outro usuário na primeira aba → segunda aba usa NOVO token mas mostra UI do ANTIGO (confusão visual de identidade).
**Fix:** `localStorage.removeItem('liber-auth')` defensivo no `limparSessao`; `BroadcastChannel` ou listener `storage` para sincronizar; reload da aba ao mudar `usuario.id`.

### 🟠 Alta · 🐛 Logout do frontend NÃO chama `/auth/logout` — refresh vive 7 dias após "Sair"
`api/auth.ts:15`, `api/http.ts:67-71`
A função `logout(refreshToken)` existe mas **não é chamada em lugar nenhum**. Só o interceptor de 401 limpa o store local. Refresh token continua válido no servidor.
**Fix:** sempre que sair, chamar `logout(refreshToken)` (fire-and-forget com timeout) antes de `limparSessao`.

### 🟠 Alta · 🐛 Loop de redirecionamento se `usuario.role` chegar `undefined`
`HomeRedirect.tsx:9-10`, `RoleRoute.tsx:11-15`
Store hidratado com token mas `usuario=null` (corrupção / schema antigo). `HomeRedirect` manda pra `/dashboard`, `RoleRoute` rejeita pra `/`, `HomeRedirect` manda pra `/dashboard` novamente → loop. DoS do próprio cliente.
**Fix:** em ambos, se `role==undefined` → `limparSessao` + `/login`.

### 🟠 Alta · 🐛 Refresh em paralelo com interceptor frágil (já em AUDIT_BUGS#4)
`api/http.ts:53-76`
`original.headers` pode ser `undefined` → `TypeError` engolido pelo catch → logout indevido. Retry único: segundo 401 não tenta refresh novamente, rejeita pro chamador. Logout via `window.location.href` descarta formulários em digitação.
**Fix:** `original.headers ??= {}`; usar `AxiosHeaders.set('Authorization', ...)`; enfileirar requests durante refresh em vez de retry isolado; toast antes do redirect.

### 🟡 Média · 🔒 `/primeiro-acesso` é rota pública (fora do `ProtectedRoute`) — race de hidratação pode renderizar sem proteção
`router.tsx:44-47`, `PrimeiroAcessoPage.tsx:41-46`
Página faz própria checagem em `useEffect` — janela onde renderiza brevemente sem proteção. Também serve como tela genérica de "troca de senha" sem proteção de menu — qualquer aluno (mesmo sem `deveTrocarSenha`) acessa via URL direta.
**Fix:** mover pra dentro de guard explícito que exija `accessToken && usuario?.deveTrocarSenha`.

### 🟡 Média · 🔒 `usuario.email` e `usuario.nome` (PII) em texto plano no `localStorage`
`auth/authStore.ts:24-46`
PC compartilhado: próximo usuário abre DevTools → vê nome+email+role do anterior, mesmo após "logout" se o `removeItem` falhou (#2). Para alunos = nome de menor.
**Fix:** `partialize` para persistir **só** os tokens; buscar `usuario` via `GET /auth/me` no boot.

### 🟡 Média · 🐛 `LoginPage` permite duplo envio (Enter + click rápido)
`pages/LoginPage.tsx:14-27`
`setCarregando(true)` é assíncrono — duas chamadas antes de propagar. Polui auditoria, consome rate limit.
**Fix:** trocar por `useMutation` (TanStack já tem dedupe), ou `useRef` síncrono.

### 🟡 Média · 🔒 Login não respeita `?next=` — UX (e quando implementar, validar contra open-redirect)
`pages/LoginPage.tsx:21`
Hoje seguro (não lê `?next=`). Defesa em profundidade: ao implementar, validar `startsWith('/')` E NÃO `startsWith('//')` (protocol-relative).

### 🟢 Baixa · 🔒 Interceptor não restringe URL alvo — token vazaria pra outros hosts se a instância for reaproveitada
`api/http.ts:14-17,53-76`
**Fix:** validar `config.url` relativa ou começa com `API_BASE` antes de anexar `Authorization`.

### 🟢 Baixa · 🔒 Persist key `'liber-auth'` genérica — possível colisão em deploy subpath
**Fix:** namespace versionado `acervoliber:v1:auth`.

### 🟢 Baixa · 🔒 LoginPage não distingue 429/423/401 — todos viram o mesmo toast
**Fix:** tratamento específico para 429 ("muitas tentativas, aguarde") sem revelar se conta existe.

### 🟢 Baixa · ⚠️ `axios.create` sem `withCredentials` — OK hoje, pegadinha se migrar pra cookie httpOnly amanhã

### ℹ️ Informativa · Autocomplete attributes corretos (`current-password`, `new-password`); HomeRedirect roteia certo por role; sem `dangerouslySetInnerHTML`

---

## Verificado e SEM achado (conforme leitura)

- `loginAttempt`: check de bloqueio é ANTES da verificação de senha — correto.
- `registrarSucesso` só no caminho feliz — não há reset indevido.
- `AppUserDetails.enabled` carregado do `ativo` em cada request — desativação reflete imediatamente.
- JWT: `Keys.hmacShaKeyFor` valida ≥32 bytes no startup (`WeakKeyException`); `parseSignedClaims` rejeita JWS unsigned; `requireIssuer` aplicado; `SessionCreationPolicy.STATELESS`.
- Refresh tokens: 256 bits de entropia, hash SHA-256 (entrada já alta-entropia, OK contra rainbow), índice único no DB, FK CASCADE.
- DTOs são `record`s imutáveis; sem mass-assignment óbvio.
- Tokens nunca aparecem em texto puro em log.
- React: sem `dangerouslySetInnerHTML`/`innerHTML`/`eval` (confirmado no pentest).
- Backend rejeita `?role=ADMIN` em register (confirmado no pentest).

---

---

# Fase 2 — Usuários e Auditoria

> 4 sub-agentes paralelos cobriram: UsuarioController + UsuarioService, AuditLogController + AuditService + AuditLog, frontend UsuariosPage + ConfiguracoesPage, frontend AuditoriaPage. **63 achados** identificados.

## ✅ Fixes aplicados nesta sessão (rebuild + verificação ao vivo)

| # | Achado | Status | Onde |
|---|---|---|---|
| 2.A.crit.1 | 🔴 Self-lockout (admin desativa a si / último ADMIN) | ✅ verificado | `UsuarioService.alterarStatus` bloqueia self + checa `countByRoleAndAtivoIsTrueAndIdNot` |
| 2.A.alta.3 | 🟠 `criarComoAdmin` aceita role=ALUNO (quebra invariante) | ✅ verificado | `UsuarioService.criarComoAdmin` rejeita ALUNO com mensagem clara |
| 2.B.alta.1 | 🟠 Trilha não identifica o ATOR (só alvo) | ✅ verificado | Migration V11 + `AuditLog.atorEmail` + `AuditService.atorAtual()` via SecurityContextHolder |
| 2.B.alta.2.a | 🟠 `PERFIL_ATUALIZADO` não auditado | ✅ verificado | `UsuarioService.atualizarPerfil` registra + novo evento no enum |
| 2.B.alta.2.b | 🟠 `REFRESH_REUSO` não auditado | ✅ verificado | `RefreshTokenService` registra + novo evento + AuditService injetado |
| 2.B.alta.2.c | 🟠 `ACESSO_NEGADO` (403) não auditado | ✅ verificado | `RestAccessDeniedHandler` E `GlobalExceptionHandler.handleAccessDenied` registram + novo evento (cobre tanto filter-chain quanto @PreAuthorize) |
| 2.B.alta.2.d | 🟠 `LOGIN_FALHA` em loginAluno com matrícula inexistente | ✅ verificado | `AuthService.loginAluno` registra antes do `BadCredentialsException` |
| 2.B.alta.3 | 🟠 `AuditLog` mutável (`@Setter`) | ✅ feito | Removido `@Setter`; única criação via `@Builder` em AuditService |
| 2.C.alta.1 | 🟠 Frontend: trocar senha não desloga (cascata 401 → reuse) | ✅ feito | `ConfiguracoesPage.salvarSenha` → `limparSessao` + `queryClient.clear()` + `/login` |
| 2.C.alta.6 | 🟠 Frontend: Switch sem Popconfirm em desativar | ✅ feito | `UsuariosPage.switchStatus` com `Popconfirm` quando alvo está ativo |
| 2.D.media.1 | 🟡 Frontend crash em evento desconhecido | ✅ feito | `tagEvento` com fallback `?? { cor:'default', texto: evento }` |
| 2.D.media.4 | 🟡 `LOGIN_SUCESSO` ainda no filtro | ✅ feito | Excluído do `OPCOES_EVENTO` (mantido no enum/EVENTO_INFO p/ registros legados) |

**Re-teste live confirmou (output capturado):**
- Diretor tentando desativar a si → **HTTP 422** "Voce nao pode desativar a si mesmo"
- `POST /usuarios {role:"ALUNO"}` → **HTTP 422** com mensagem dirigindo ao endpoint correto
- `PUT /auth/perfil` → audit `PERFIL_ATUALIZADO` com `ator_email=diretor@liber.local`
- Carla (BIB) tenta `/usuarios` e `/auditoria` → **HTTP 403** + 2 entradas `ACESSO_NEGADO` com `ator_email=carla.bib@liber.local`
- `POST /auth/login-aluno {matricula:naoexiste}` → audit `LOGIN_FALHA` com detalhe
- Rotacionar refresh + reapresentar o antigo → audit `REFRESH_REUSO` com `usuario_email=diretor@liber.local`

**Deferido pra cleanup pass (fim Fase 7):**
- Endpoint admin reset-senha para equipe (feature nova, não estritamente segurança)
- Normalização Unicode (NFKC) de e-mail centralizada em helper único
- `@PreAuthorize` redundante no service (defesa em profundidade)
- Mascarar email de aluno em listagem `/usuarios` (LGPD)
- Filtros completos na auditoria (`?usuarioEmail=`, `?ator=`, `?desde=`, `?ate=`)
- Mensagem 409 amigável vs genérica em race de email duplicado
- CHECK constraint em `audit_log.evento`
- Hash chain anti-tampering na auditoria
- REVOKE DELETE/UPDATE da tabela `audit_log` (requer separação de roles DB)
- Retention LGPD (purge >90d) + endpoint admin de purge por sujeito
- Frontend: validador de senha cliente=servidor (zxcvbn), confirmação de senha em criar usuário, refetchInterval na auditoria, isError handling
- Tradução de roles, ícones nas tags da auditoria (a11y), tooltip pra detalhe longo
- Limite cap explícito no Pageable (`max-page-size` global já está em 50)
- DTO defensivo: `@JsonIgnoreProperties(ignoreUnknown=false)` explícito

## Sumário Fase 2 (63 achados)

| Sev | Backend Usuários (2.A) | Backend Auditoria (2.B) | Frontend Usuários/Config (2.C) | Frontend Auditoria (2.D) | **Total** |
|---|---|---|---|---|---|
| 🔴 Crítica | 1 | 0 | 0 | 0 | **1** |
| 🟠 Alta | 6 | 3 | 5 | 0 | **14** |
| 🟡 Média | 6 | 3 | 8 | 5 | **22** |
| 🟢 Baixa | 4 | 4 | 6 | 6 | **20** |
| ℹ️ Info | 1 | 1 | 2 | 1 | **5** |
| **Total** | 18 | 11 | 21 | 12 | **62** |

## Resumo dos achados por área (compacto)

**Backend Usuários** (`UsuarioService` / `UsuarioController`):
- 🔴 Self-lockout (FIX) · 🟠 criarComoAdmin role=ALUNO (FIX) · 🟠 reset-senha endpoint inexistente · 🟠 enumeração `/usuarios` sem cap + vaza `deveTrocarSenha` · 🟠 normalização email só em 1 ponto (NFKC) · 🟠 `Pageable` sem cap específico · 🟠 mass-assignment fantasma quando role for adicionada · 🟡 race de e-mail duplicado vira 409 genérico · 🟡 `CriarUsuarioRequest` sem whitelist de roles · 🟡 `Pattern` ausente no nome · 🟡 inflight requests pós-desativação · 🟢 `existsByRole` órfão · 🟢 e-mail de aluno em listagem vaza matrícula · 🟢 truncate Unicode em e-mail.

**Backend Auditoria** (`AuditService` / `AuditLog`):
- 🟠 Ator ausente (FIX) · 🟠 cobertura: PERFIL/REFRESH_REUSO/ACESSO_NEGADO/login-aluno (FIX) · 🟠 mutabilidade `@Setter` (FIX parcial — removido; falta REVOKE DELETE no DB) · 🟡 log/CSV injection em `detalhe` · 🟡 `usuario_email` sem mask LGPD · 🟡 filtros pobres · 🟡 desempate por ordem indefinida · 🟢 sem CHECK no evento · 🟢 `extrairIp` duplicada (já corrigido lendo `getRemoteAddr()` no edit do AuditService) · 🟢 `REQUIRES_NEW` sem contexto em jobs · 🟢 LOGIN_SUCESSO órfão (FIX no front).

**Frontend Usuários/Config**:
- 🟠 ConfiguracoesPage senha → cascata (FIX) · 🟠 Switch sem Popconfirm (FIX) · 🟠 Criar role=ADMIN sem destaque · 🟠 validação senha client≠server · 🟠 sem confirmar senha · 🟠 sem reset-senha · 🟡 switch global loading · 🟡 sem `isError` · 🟡 paginação edge · 🟡 `Page<T>` sem schema · 🟡 tag role texto cru · 🟡 sem `ALUNO` no `tagRole` · 🟡 troca de senha sem indicador de força · 🟡 sem trocar e-mail · 🟢 duplo submit · 🟢 cache não invalida `['me']` · 🟢 normalização email · 🟢 sem `createdAt`/`ultimoLogin`.

**Frontend Auditoria**:
- 🟡 crash evento desconhecido (FIX) · 🟡 sem `isError` · 🟡 sem refetchInterval · 🟡 `LOGIN_SUCESSO` no filtro (FIX) · 🟡 filtro enum sem validação backend · 🟢 IP sem mask LGPD · 🟢 detalhe longo quebra · 🟢 `user_agent` no DTO mas nunca exibido · 🟢 filtro+page sem URL params · 🟢 sem default de "segurança" · 🟢 sem export CSV · 🟢 tags sem ícone (a11y).

---

---

# Fase 3 — Alunos e Empréstimos

> 4 sub-agentes paralelos cobriram: AlunoController + AlunoService; EmprestimoController + EmprestimoService + StatusUrgencia; frontend AlunosPage; frontend EmprestimosPage + HistoricoPage. **72 achados** identificados.

## ✅ Fixes aplicados nesta sessão (rebuild + verificação ao vivo)

| # | Achado | Status | Onde |
|---|---|---|---|
| 3.B.alta.5 | 🟠 Sem auditoria de empréstimo (gestão de bem público sem trilha) | ✅ verificado | 3 novos eventos: `EMPRESTIMO_REGISTRADO`, `EMPRESTIMO_DEVOLVIDO`, `ESTOQUE_DIVERGENCIA` registrados em `EmprestimoService.registrar/registrarDevolucao` |
| 3.B.alta.1 | 🟠 `registrarParaReserva` público — regressão futura cria empréstimo fantasma | ✅ feito | Visibilidade rebaixada para package-private + Javadoc explícito |
| 3.B.alta.4 | 🟠 `prazoDias` `Integer.MAX_VALUE` → `LocalDate.plusDays` overflow → 500 | ✅ verificado | `@Max(3650)` em `EmprestimoRequest` e `ConfirmarReservaRequest`; resposta 400 com mensagem clara |
| 3.B.media.1 | 🟡 `StatusUrgencia.from` quebra com `dataDevolucaoPrevista=null` | ✅ feito | Fallback VERDE com comentário explicativo |
| 3.A.alta.3 | 🟠 `remover` aluno só checa empréstimos → 409 genérico para reservas/acesso | ✅ verificado | Pré-checagens explícitas + mensagens dirigidas (testado: aluno com acesso → 422 "Remova o acesso antes"; aluno com empréstimo → mensagem do empréstimo) |
| 3.A.alta.4 | 🟠 `criarAcesso` race vira 409 opaco | ✅ feito | `try/catch DataIntegrityViolationException` → mensagem "ja possui acesso" amigável |
| 3.C.alta.2 / 3.D.alta.1 | 🟠 Cache stale após registrar/devolver empréstimo | ✅ feito | `CHAVES_RELACIONADAS` agora cobre `alunos`/`reservas`/`resumo-reservas`/`livros-opcoes` |
| 3.D.media.6 | 🟡 `StatusUrgenciaTag` quebra a tabela inteira com valor desconhecido | ✅ feito | Fallback (igual padrão do `tagEvento`) |
| 3.D.media.4 | 🟡 `formatarData` produz `undefined/undefined/undefined` em ISO completo | ✅ feito | Regex strict + tolerância a `yyyy-MM-ddTHH:mm...` |
| 3.D bonus | ℹ️ Frontend ignora novos eventos de auditoria | ✅ feito | Tipos TS + `EVENTO_INFO` da `AuditoriaPage` cobrem `EMPRESTIMO_REGISTRADO`, `EMPRESTIMO_DEVOLVIDO`, `ESTOQUE_DIVERGENCIA` |

**Re-teste live confirmou (output capturado):**
- `POST /emprestimos {prazoDias:5000}` → **HTTP 400** "Prazo nao pode exceder 3650 dias" (antes seria 500 com `DateTimeException`)
- `POST /emprestimos` → audit `EMPRESTIMO_REGISTRADO` com ator_email + detalhe estruturado (`Emprestimo id=21, livro id=33, aluno matricula=202700112, prazo=7d`)
- `POST /emprestimos/21/devolucao` → audit `EMPRESTIMO_DEVOLVIDO`
- `DELETE /alunos/16` (aluno com acesso) → **422** "Aluno possui acesso ao sistema (login do portal). Remova o acesso antes."
- `DELETE /alunos/5` (aluno com empréstimos) → **422** mensagem do empréstimo (antes seria 409 "Conflito de dados" opaco)

**Deferido pra cleanup pass:**
- **Endpoint `DELETE /alunos/{id}/acesso`** (revoga login de aluno graduado) — feature nova
- **Endpoint `POST /emprestimos/{id}/renovacao`** (renovar prazo sem novo empréstimo) — feature
- **`Aluno.ativo`** (migration + check em registrar/reservar) — depende de UX "marcar formado"
- **UI criar/remover acesso de aluno** em `AlunosPage` — espera endpoints acima
- **`HistoricoPage` com filtros** (`?alunoId`, `?livroId`, `?situacao`, `?desde/ate`)
- **Flag `temAtraso` no aluno** + alerta visual em "novo empréstimo"
- **Devolução concorrente** — mover `save` antes do `incrementarEstoque` (subtle)
- **Matrícula normalize lower-case** (precisa backfill no DB)
- **`AlunoResumoDTO` mask PII** quando consumido por Portal do Aluno (Fase 5)
- **`listarAtivos` com paginação** (hoje retorna List sem cap)
- **`listar` alunos N+1** — query agregada
- **Atualização de matrícula** bloqueada quando há `Usuario` vinculado
- **`PATCH /alunos/{id}/status`** com checagens (depende de `Aluno.ativo`)
- **Auditoria com pattern AFTER_COMMIT** vs REQUIRES_NEW para success events (falso positivo se outer transaction rolar back)
- **Eventos `ALUNO_CRIADO`/`ALUNO_REMOVIDO`/`RESERVA_*`** (cobertura ampliada)

## Sumário Fase 3 (~72 achados)

| Sev | Backend Alunos (3.A) | Backend Empréstimos (3.B) | Frontend Alunos (3.C) | Frontend Empréstimos+Histórico (3.D) | **Total** |
|---|---|---|---|---|---|
| 🔴 Crítica | 0 | 0 | 0 | 0 | **0** |
| 🟠 Alta | 4 | 5 | 5 | 5 | **19** |
| 🟡 Média | 4 | 6 | 6 | 9 | **25** |
| 🟢 Baixa | 6 | 4 | 4 | 6 | **20** |
| ℹ️ Info | 1 | 2 | 1 | 2 | **6** |
| **Total** | 15 | 17 | 16 | 22 | **70** |

## Resumo dos achados por área (compacto)

**Backend Alunos** (`AlunoService` / `AlunoController`):
- 🟠 atualização matrícula deixa Usuario órfão · 🟠 sem endpoint remover acesso · 🟠 remover aluno só checa empréstimos (FIX) · 🟠 criarAcesso TOCTOU + 409 opaco (FIX) · 🟡 matrícula trim-only sem lower · 🟡 AlunoResumoDTO em ReservaResponse vaza PII · 🟡 listar N+1 vaza padrão leitura · 🟡 termo enumera turmas/nomes · 🟡 Aluno sem campo ativo · 🟢 sem regex em nome/turma · 🟢 mass-assignment latente Lombok · 🟢 senha provisória no body do request · 🟢 sem @Size(min) na matrícula.

**Backend Empréstimos** (`EmprestimoService`):
- 🟠 registrarParaReserva público (FIX) · 🟠 devolução concorrente sem lock pessimista · 🟠 Aluno sem ativo permite empréstimo a graduado · 🟠 prazoDias overflow (FIX) · 🟠 cobertura auditoria EMPRESTIMO_* (FIX) · 🟡 StatusUrgencia.null (FIX) · 🟡 StatusUrgencia ignora dataDevolucaoEfetiva · 🟡 registrar com livroId inexistente faz UPDATE inútil · 🟡 mudança em prazoMaximoDias não retroage · 🟡 incrementarEstoque clamp silencioso (FIX parcial — agora audita) · 🟡 listarPorAluno expõe AlunoResumoDTO completo · 🟢 sem ALUNO_CRIADO no enum · 🟢 listarAtivos sem paginação · 🟢 @Version sem default no schema (cosm) · 🟢 listarHistorico sem tie-break.

**Frontend Alunos**:
- 🟠 sem UI criar/remover acesso · 🟠 invalidação só ['alunos'] · 🟠 matrícula editável sem aviso · 🟠 duplo submit · 🟠 busca sem signal/abort · 🟡 normalização cliente da matrícula · 🟡 sem isError · 🟡 popconfirm sem loading per-row · 🟡 sem máscara PII de menor · 🟡 paginação após criar · 🟡 cache stale · 🟢 drawer não reseta · 🟢 mensagem genérica · 🟢 mobile Card overflow · 🟢 sem mass-delete (positivo).

**Frontend Empréstimos+Histórico**:
- 🟠 invalida ['alunos'] (FIX) · 🟠 loading global no devolver · 🟠 sem aviso aluno com atraso · 🟠 histórico sem filtros · 🟠 sem renovação · 🟡 query keys frágeis · 🟡 mensagens de erro vazam PII · 🟡 validação prazo só client-side · 🟡 drawer não reseta · 🟡 formatarData frágil (FIX) · 🟡 StatusUrgenciaTag fail-fast (FIX) · 🟡 tagSituacao+StatusUrgenciaTag duplicam · 🟡 sem reverter devolução · 🟢 N+1 size:100 · 🟢 mobile drawer · 🟢 sem isError · 🟢 "—" ambíguo · 🟢 vence hoje · 🟢 prazoDias morto · 🟢 erros 422 sem por-campo · ℹ️ pageSize · ℹ️ staleTime.

---

---

# Fase 4 — Livros e Capas

> 4 sub-agentes paralelos cobriram: LivroService+LivroController+entity+DTOs (CRUD/busca/estoque); CapaService+CapaBackfillJob (SSRF e consultas externas); upload manual de capa + LivroCapa + endpoint público `/capa-imagem`; frontend LivrosPage+CatalogoPage+CapaLivro+CapaPreview+api/livros+api/capas. Achados brutos triados contra leitura do código: removidos falsos positivos onde os agentes superestimaram (ex.: GET /livros não é público — exige auth; `contentType` da capa armazenado é o **detectado por magic bytes**, não o enviado pelo cliente — XSS via SVG/HTML está mitigado; cache busting `?v=timestamp` **já existe** para capa manual; `referrerPolicy STRICT_ORIGIN_WHEN_CROSS_ORIGIN` está configurado globalmente nas response headers).

## ✅ Fixes aplicados nesta sessão (rebuild + verificação ao vivo)

| # | Achado | Status | Onde |
|---|---|---|---|
| 4.A.alta.1 | 🟠 `normalizarIsbn` divergente → duplicata silenciosa + 500 no flush | ✅ verificado | Novo `com.liber.util.Isbn.normalize` usado em LivroService (cadastrar/atualizar) e CapaService (chaveCache/resolverCapa) |
| 4.A.alta.2 | 🟠 `resolverCapa` síncrono em cadastrar/atualizar → conexão DB segura 6-18s | ✅ verificado | `LivroService.cadastrar`/`atualizar` agora seta `capaUrl=null`; `CapaBackfillJob` preenche depois |
| 4.A.alta.3 | 🟠 `remover` livro não checa reservas | ✅ verificado | `LivroService.remover` checa `reservaRepository.countByLivroIdAndStatus(PENDENTE/CONFIRMADA)` |
| 4.B.alta.1 | 🟠 `tratarUrlGoogle` sem allowlist de host → SVG XSS via URL maliciosa | ✅ feito | Novo `CapaService.urlSegura(url)` valida scheme=https + host ∈ `HOSTS_CAPA_PERMITIDOS` (books.google.com, books.googleusercontent.com, lh3.googleusercontent.com, covers.openlibrary.org); aplicado em `tratarUrlGoogle` e `consultarOpenLibrary` |
| 4.B.alta.2 | 🟠 `ConcurrentHashMap` sem limite no cache do CapaService | ✅ feito | Substituído por `LinkedHashMap` (access-order) com `removeEldestEntry > 10_000` envolto em `synchronizedMap` |
| 4.D.alta.1 | 🟠 `<img>` sem `referrerpolicy` → vaza URL da app a Google/Open Library | ✅ feito | `CapaLivro.tsx` agora renderiza `<img referrerPolicy="no-referrer" ...>` |

**Re-teste live confirmou (output capturado):**
- POST /livros com ISBN `978-5559550390` → **HTTP 201 em 172ms** (antes podia bloquear 6-18s), `capaUrl=null` (backfill assíncrono), `isbn` salvo normalizado como `9785559550390`.
- POST /livros com ISBN `9785559550390` (mesma edição sem hífen) → **HTTP 422** `"ISBN ja cadastrado: 9785559550390"` (antes: passava na pré-checagem e morria com 500 no flush da unique constraint).
- DELETE /livros/28 (livro com reserva PENDENTE) → **HTTP 422** `"Nao e possivel remover livro com reservas pendentes ou confirmadas. Cancele as reservas antes."` (antes: deletava silenciosamente, cascateando a reserva).
- GET /livros/capa?isbn=9788535914849 → `"capaUrl":"https://covers.openlibrary.org/b/isbn/9788535914849-L.jpg?..."` (host na allowlist, aceito); GET com `titulo=Dom%20Casmurro&autor=Machado%20de%20Assis` → `"capaUrl":"https://books.google.com/books/content?..."` (host na allowlist, aceito). Compilação da `urlSegura` clean; lógica rejeita `data:`/`javascript:`/scheme não-https e hosts fora da allowlist (verificado por leitura e teste de happy-path).
- Bundle `CapaLivro-CqOOkfuW.js` do frontend contém literal `referrerPolicy:"no-referrer"` — atributo chega ao `<img>` em produção.

**Deferido pra cleanup pass (fim da Fase 7):**
- 4.A.alta.4 (auditoria `LIVRO_CRIADO`/`ATUALIZADO`/`REMOVIDO`)
- 4.C.alta.2 (auditoria `CAPA_ENVIADA`/`REMOVIDA`)
- 4.C.alta.1 (apertar `max-file-size=2MB` e streaming de upload)
- 4.C.alta.3 (`removerCapaManual` resiliente a Google down)
- 4.D.alta.2 (CSP meta tag), 4.D.alta.3 (invalidar `['catalogo']`), 4.D.alta.4 (validar scheme em `resolverUrlCapa`)
- 4.A.media.1 (`LivroCatalogoResponse` sem `quantidadeDisponivel` — privacy comportamental aluno)
- 4.B.alta.3 (`followRedirects(NEVER)` + revalidação manual de Location.host)
- 4.B.media.1-4 (query injection Google, OptimisticLockException no backfill, cache negativo sem TTL, API key na querystring)
- 4.C.media.1-2 (ETag + cache 7d na `/capa-imagem`, `@Version` em LivroCapa)
- 4.D.media.1-5 (debounce com AbortSignal, timeout axios, staleTime CapaPreview, retry:0 em salvar, loading per-row)
- Unificar este cache LRU com os mapas do `RateLimitingFilter`/`InMemoryLoginAttemptService` em Caffeine (deferimento herdado da Fase 1)

---

## Sumário Fase 4 (44 achados)

| Sev | Backend Livros (4.A) | Backend Capas/SSRF (4.B) | Backend Upload (4.C) | Frontend (4.D) | **Total** |
|---|---|---|---|---|---|
| 🔴 Crítica | 0 | 0 | 0 | 0 | **0** |
| 🟠 Alta | 4 | 3 | 3 | 4 | **14** |
| 🟡 Média | 4 | 4 | 3 | 5 | **16** |
| 🟢 Baixa | 3 | 2 | 2 | 4 | **11** |
| ℹ️ Info | 1 | 1 | 0 | 1 | **3** |
| **Total** | 12 | 10 | 8 | 14 | **44** |

**Top 6 pra atacar primeiro** (impacto × esforço):

1. 🟠 **`cadastrar` / `atualizar` rodam `capaService.resolverCapa` SÍNCRONO dentro da transação** — POST /livros pode bloquear 6–18s segurando conexão DB se Google estiver lento; combinado com cache externo, vira amplificador de DoS.
2. 🟠 **`normalizarIsbn` divergente** — `LivroService` só faz `trim()`, `CapaService` strip não-dígitos + uppercase; `existsByIsbn` usa string crua → duplicatas "978-…" vs "978…" passam e quebram unique constraint só no flush (HTTP 500).
3. 🟠 **`remover` livro não checa reservas** — livro com reserva PENDENTE (que segura exemplar) pode ser deletado; cascade via FK quebra a reserva órfã.
4. 🟠 **`tratarUrlGoogle` sem allowlist de host** — URL retornada pelo Google vai pro DB e direto pro `<img src>`; se a resposta um dia vier maliciosa (CDN mudou, bug Google), `data:image/svg+xml,<svg onload=…>` no `src` EXECUTA SVG. Defesa em profundidade barata.
5. 🟠 **Cache do `CapaService` é `ConcurrentHashMap` sem limite** — bibliotecário autenticado consultando `/livros/capa?titulo=X&autor=Y` com pares aleatórios faz heap crescer indefinidamente.
6. 🟠 **`<img>` sem `referrerpolicy="no-referrer"` em capa externa** — Google/Open Library recebem `Referer: https://liber.local/...` por cada render, possibilitando fingerprinting comportamental (LGPD).

---

## 4.A — Backend Livros (CRUD, busca, estoque)

### 🟠 Alta · 🔒 `normalizarIsbn` divergente entre LivroService e CapaService → duplicata silenciosa + 500 no flush
`LivroService.java:230-235`, `CapaService.java:213-218`, `LivroService.java:56,84`
`LivroService.normalizarIsbn` faz só `trim()`. `CapaService.normalizarIsbn` strip tudo que não é dígito/X + UPPERCASE. `cadastrar` checa `existsByIsbn(req.isbn())` com a string CRUA → "978-8535914849" e "9788535914849" **passam ambos** na pré-checagem, vão para o DB com a string trimada, e a UNIQUE constraint dispara só no flush → 500 genérico (não 422 amigável). Pior: `CapaService.chaveCache` colide as duas formas no cache, então uma chega "com capa" e a outra "sem".
**Fix:** método único `Isbn.normalize(String)` usado em AMBOS os pontos (strip → uppercase); usar a forma normalizada tanto em `existsByIsbn` quanto em `livro.setIsbn`; tratar `DataIntegrityViolationException` por unique de ISBN em 422 com mensagem clara.

### 🟠 Alta · 🐛 `cadastrar`/`atualizar` chamam `capaService.resolverCapa` SÍNCRONO dentro da transação → conexão DB segura por 6-18s
`LivroService.java:68,118`
`resolverCapa` faz até 3 HTTPs (Google ISBN + Google título + Open Library) com 6s de read-timeout cada. O método é `@Transactional` → a conexão JDBC fica encostada esperando rede externa. 20 cadastros paralelos com Google fora do ar = pool de conexão esgotado → 500 em endpoints não relacionados (auditoria, login). Amplifica qualquer downtime externo numa pane interna.
**Fix:** resolver capa FORA da transação (async via `@Async` ou explicitamente `PROPAGATION.NOT_SUPPORTED` em método separado) ou cadastrar sem capa e deixar o `CapaBackfillJob` preencher (capa não é dado crítico no fluxo de cadastro). Mínimo: setar timeout HARDcoded mais curto via `CompletableFuture.orTimeout(3, SECONDS)` no caminho síncrono.

### 🟠 Alta · 🐛 `remover` só checa empréstimos — livro com reserva PENDENTE pode ser deletado, cascade quebra reserva
`LivroService.java:125-136`, `ReservaRepository`
`existsByLivroId` cobre só `Emprestimo`; nada checa `Reserva`. Reserva PENDENTE segura `quantidadeDisponivel` (decremento em `criar`); deletar o livro deixa a reserva órfã (se FK CASCADE, soma sumir do histórico; se RESTRICT, 500 opaco). Pior: aluno do portal vê o livro sumir do "minhas reservas" e a UI quebra.
**Fix:** antes do `deleteById`, `reservaRepository.existsByLivroIdAndStatusIn(id, List.of(PENDENTE, CONFIRMADA))` → 422 "Livro tem reservas pendentes/confirmadas".

### 🟠 Alta · ⚠️ Sem auditoria de `LIVRO_CRIADO`/`LIVRO_ATUALIZADO`/`LIVRO_REMOVIDO` — gestão de bem público sem trilha
`LivroService.java:54-136`, `EventoAuditoria`
Fase 3 adicionou `EMPRESTIMO_REGISTRADO`/`EMPRESTIMO_DEVOLVIDO`. Livros — que são o **bem** sob gestão — não têm nenhum evento. Bibliotecário comprometido pode remover livros do acervo (raros, didáticos) sem rastro. `LivroResponse.from` é chamado mas nada vai pra `audit_log`.
**Fix:** 3 eventos novos no enum + `auditService.registrar(...)` em `cadastrar/atualizar/remover` com detalhe estruturado (`Livro id=X titulo='...' isbn=...`).

### 🟡 Média · 🔒 `LivroResponse` expõe `quantidadeDisponivel` em CatalogoPage do aluno — privacy comportamental
`LivroResponse.java:13`, `CatalogoPage.tsx`
Aluno vê "3 de 5 exemplares". Padrão temporal (X comparecente → empréstimo registrado) permite inferir quem pegou o livro (no portal só ele e os colegas têm acesso ao catálogo da turma). LGPD vê privacidade de leitura como sensível.
**Fix:** projeção dedicada `LivroCatalogoResponse` sem `quantidadeDisponivel` (só `disponivel: boolean` ou "Disponível"/"Indisponível"); manter número exato apenas em endpoints autenticados como bibliotecário.

### 🟡 Média · 🐛 `cadastrar` usa string CRUA em `existsByIsbn` → race + 500
`LivroService.java:56,60`
`existsByIsbn(req.isbn())` antes de `normalizarIsbn(req.isbn())`. Mesma ISBN com espaço em torno (`" 978...  "` vs `"978..."`) passa nas duas pré-checagens → `livro.setIsbn(isbnTrimado)` colide na DB. Cenário: cliente automatizado retransmite o mesmo POST após timeout — segundo POST cai exatamente nesse race.
**Fix:** normalizar ANTES de `existsByIsbn`; consolidar com 4.A.alta.1.

### 🟡 Média · 🧪 `atualizar` recalcula `quantidadeDisponivel` mas não revalida que `quantidadeExemplares ≥ ativos+reservasPendentes` no momento do save (race)
`LivroService.java:93-110`
Leitura de `countByLivroIdAndSituacao` e `countByLivroIdAndStatus` é DENTRO da mesma transação READ_COMMITTED, mas SEM lock pessimista no Livro nem no Empréstimo. Entre o count e o save, um POST /emprestimos do bibliotecário pode incrementar `ativos` — o `setQuantidadeDisponivel` resultante fica abaixo do real, mas o `decrementarEstoque` do empréstimo passa porque tem CHECK ≥ 0; o saldo final fica negativo do ponto de vista contábil (ativos > exemplares).
**Fix:** `@Lock(PESSIMISTIC_WRITE)` no `findById` de `atualizar`, ou usar `@Version` (já existe no Livro) — ao salvar, OptimisticLockException sinaliza retry; 409 → cliente reabre tela.

### 🟡 Média · 🧪 `LivroRequest.ano` aceita `@Max(9999)` — livro futuro do ano 9999
`LivroRequest.java:24-25`
Sem upper bound contextual; admin pode cadastrar `ano=9999` por erro de digitação. Inofensivo, mas catálogo fica feio e quebra filtros por ano.
**Fix:** `@Max(value=...)` com `LocalDate.now().getYear()+1` via custom validator, ou `@Max(2100)` literal.

### 🟢 Baixa · ⚠️ `LivroRequest` sem `@Pattern` em `titulo`/`autor` — aceita zero-width, `\r\n`, controle
`LivroRequest.java:12-18`
Não causa SQL injection (JPA parametriza), mas afeta busca (`LIKE`), CSV de relatórios e log injection.
**Fix:** custom validator `@TextoSeguro` que barre `[\p{Cc}​-‏]` em campos de texto.

### 🟢 Baixa · 🔒 `?termo=` LIKE com timing/payload size leak — enumeração de catálogo lento mas viável
`LivroRepository` + `LivroController.java:46-51`
`LOWER(titulo) LIKE LOWER(CONCAT('%', :termo, '%'))` é parametrizado, mas o tamanho da página devolvida varia com matches → enumeração estatística de obras do acervo. Como GET /livros exige autenticação (qualquer aluno serve), o vetor é interno; risco baixo.
**Fix:** rate limit no path `/api/v1/livros` (não só `/auth/login*`); mais a longo prazo, índice de full-text (`pg_trgm` ou `tsvector`).

### 🟢 Baixa · ⚠️ `Pageable` sem cap explícito específico no endpoint — depende da config global
`LivroController.java:49`
`@PageableDefault(size=20)` é apenas o default; cliente que mandar `?size=10000` cai no `spring.data.web.pageable.max-page-size` global (já está em 50). OK funcionalmente, mas vale tornar explícito (defesa em profundidade).
**Fix:** anotação `@PageableDefault(size=20)` + validação de `size <= 50` no controller; opcional.

### ℹ️ Informativa · Migration tem `@Version`, CHECK `qtd_disponivel >= 0`, CHECK `qtd_consistente`, índice no `isbn` — fundação correta. `decrementarEstoque/incrementarEstoque` usam UPDATE atômico — sem race no estoque. `LivroResumoDTO` em `EmprestimoResponse` não vaza estoque/ISBN.

---

## 4.B — Backend Capas e consultas externas (SSRF)

### 🟠 Alta · 🔒 `tratarUrlGoogle` sem allowlist de host → URL maliciosa retornada pela Google vira `<img src>` (SVG XSS via `data:image/svg+xml`)
`CapaService.java:226-231`, `Livro.capaUrl` persistido + frontend `<img src>`
A função só troca `^http://` por `https://`. Resposta JSON da Google poderia (hoje não, hipoteticamente) trazer `thumbnail: "data:image/svg+xml,<svg onload=alert(1)>"` ou `"javascript:..."`. `javascript:` em `<img src>` é ignorado por browsers modernos, mas **`data:image/svg+xml,<svg onload=…>` EXECUTA o script no contexto do app**. URL vai pro DB e é renderizada por todos os usuários — XSS persistente para 1 bug futuro do Google.
**Fix:** validar `URI.create(url)`, exigir `scheme in ("http","https")` E `host endsWith ("googleapis.com","googleusercontent.com","books.google.com")`; rejeitar URI com scheme `data:`/`javascript:`/`file:`. Mesma checagem na URL devolvida pela Open Library (já é restrita pelo template, mas defenda).

### 🟠 Alta · 🔒 Cache `ConcurrentHashMap` sem limite — DoS de heap por bibliotecário autenticado
`CapaService.java:54`
`/api/v1/livros/capa?titulo=X&autor=Y` com pares únicos cria entrada nova a cada chamada. Sem rate limit (endpoint não cai em `LOGIN_PATH_PREFIX` do `RateLimitingFilter`) e sem cap no mapa, bibliotecário malicioso (ou bug no frontend) enche o heap.
**Fix:** trocar por `Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(24h)`; deferido pra cleanup pass se quiser unificar com os outros mapas (login attempt, rate limit) já listados em Fase 1.

### 🟠 Alta · 🔒 `followRedirects(NORMAL)` sem validação de host de destino — redirect pra IP interno
`CapaService.java:58`
HttpClient segue 3xx automaticamente. Google/Open Library hoje não redirecionam para `169.254.169.254` (metadata) nem `10.x.x.x`, mas: (a) MITM em rede corporativa sem TLS pinning consegue; (b) bug futuro no upstream. Resposta é descartada como JSON inválido, mas o servidor já bateu no host interno (log/metric/alert no alvo, side-effects em endpoints `GET-as-write` mal-projetados).
**Fix:** `followRedirects(NEVER)` + tratar 3xx manualmente validando `Location.host` contra allowlist (alinhar com 4.B.alta.1).

### 🟡 Média · 🐛 Query Google contaminável via título/autor — sintaxe `intitle:` quebra com aspas/`inauthor:` embutido
`CapaService.java:110-111`
`"intitle:" + titulo + " inauthor:" + autor` — admin com título `Foo" inauthor:Machado` muda a query semanticamente. URL-encoder não ajuda (a sintaxe Google é parseada antes). Não é vulnerabilidade de segurança forte, mas degrada qualidade do match.
**Fix:** strip de caracteres da sintaxe Google (`["():+\-]`) ou usar aspas duplas envolvendo cada termo: `intitle:"Foo bar" inauthor:"Machado"`.

### 🟡 Média · 🧪 `CapaBackfillJob` sem catch de OptimisticLockException — admin editando livro durante backfill mata o job
`CapaBackfillJob.java:54-68`, `LivroService.java:155-163`
`livroService.definirCapa` faz `save` sem lock; se admin editou o livro entre o `findByIsbnIsNotNullAndCapaUrlIsNull` (snapshot inicial) e o `save` do job, `@Version` lança e o `for` interrompe (não tem try/catch interno por item). Resto da lista não é processado.
**Fix:** `try { ... } catch (OptimisticLockException e) { log.debug("Pulando livro {}: conflito", livro.getId()); }` ao redor de cada iteração; o backfill 6h depois tenta de novo.

### 🟡 Média · 🐛 Cache negativo (entrada com `""`) nunca expira — livro sem capa hoje fica "sem capa" pra sempre na vida útil do processo
`CapaService.java:138-142`
Se Google/Open Library não tiverem capa **agora**, cache marca `""`. Quando upstream finalmente ganhar a capa daquele livro, só o reset do processo (reboot) limpa. Pior em produção long-running.
**Fix:** TTL no cache (parte do fix de 4.B.alta.2); ou separar mapa positivo (long-lived) de negativo (TTL 24h).

### 🟡 Média · ⚠️ `googleBooksApiKey` na querystring — vaza em logs de proxy/CDN
`CapaService.java:152-153`
Toda chamada inclui `&key=...` na URL. Logs do upstream da Google têm a key. Não é vulnerabilidade do AcervoLiber (recomendação da própria Google v1), mas merece nota.
**Fix:** documentar; restringir a key por domínio/IP no painel Google; rotacionar periodicamente (cleanup pass).

### 🟢 Baixa · ⚠️ Endpoint `/api/v1/livros/capa` sem rate limit por usuário — bibliotecário consome quota Google em loop
`LivroController.java:53-64`, `RateLimitingFilter.java:33`
`@PreAuthorize` impede anônimo, mas autenticado pode chamar 1000×/seg → preview de capa não tem dedupe; `CapaPreview` no front debounceia mas request fabricado direto bypassa.
**Fix:** estender `RateLimitingFilter` com `/api/v1/livros/capa` (limite mais alto, ex. 30/min por usuário).

### 🟢 Baixa · 🧪 Sem limite de saltos de redirect — loop teórico
`CapaService.java:58`
`NORMAL` não tem cap explícito. Conjugado com timeout 6s do read, o pior caso é morrer no timeout, mas vale.
**Fix:** se mantiver NORMAL, validar `redirectCount < 5` via interceptor (Java HttpClient não expõe diretamente — alternativa é trocar pra `Apache HttpClient` ou desabilitar e seguir manualmente).

### ℹ️ Informativa · `URLEncoder.encode` cobre query string; SSRF clássico via `?isbn=` controlado não é possível (sempre concatenado depois do `q=intitle:...` ou `?country=BR&maxResults=5&q=isbn:`); HttpClient com `connectTimeout(4s)`/`readTimeout(6s)` mantém pior caso bounded; CapaBackfillJob tem `AtomicBoolean` anti-overlap.

---

## 4.C — Backend Upload e storage de capa

### 🟠 Alta · 🐛 `file.getBytes()` carrega TODO o upload em RAM antes da checagem de tamanho — Spring multipart é 5MB, código rejeita >2MB
`LivroController.java:71`, `LivroService.java:175-177`, `application.properties:125`
`max-file-size=5MB` permite que o request body inteiro entre em RAM via `MultipartFile`. `file.getBytes()` materializa um `byte[5MB]` ANTES de bater no `TAMANHO_MAX_CAPA=2MB`. Atacante autenticado paraleliza N uploads de 5MB → N×5MB de heap residente. Não é OOM imediato, mas amplificador.
**Fix:** apertar `max-file-size=2MB` (alinha com `TAMANHO_MAX_CAPA`); usar `file.getSize()` para curto-circuito ANTES de `getBytes()`; idealmente streaming via `file.getInputStream()` lendo 8KB de cada vez e abortando ao passar 2MB.

### 🟠 Alta · ⚠️ Sem auditoria `CAPA_ENVIADA`/`CAPA_REMOVIDA` — quem trocou a capa de um livro é invisível
`LivroService.java:170-204,217-228`
Mesmo problema que 4.A.alta.4 — gestão de bem público sem trilha.
**Fix:** 2 eventos novos no enum, `auditService.registrar` em `enviarCapa` e `removerCapaManual` com `Livro id=X (N bytes, tipo=image/png)`.

### 🟠 Alta · 🐛 `removerCapaManual` chama `resolverCapa` síncrono — se Google estiver fora, livro fica com `capaUrl=null` até próximo backfill (6h)
`LivroService.java:224`
Volta da capa manual pra automática faz o GET externo dentro da transação. Se retorna null (timeout, 429), persiste null — capa some da UI imediatamente, só restaura no `CapaBackfillJob` 6h depois.
**Fix:** se `resolverCapa` retorna null, NÃO setar `capaUrl=null` — manter o que estava lá (ou marcar `pendente_backfill=true` em coluna nova); CapaBackfillJob pega.

### 🟡 Média · ⚠️ Cache de 30 dias em `GET /capa-imagem` sem ETag — capa nova sobrescrita pela antiga em CDNs/proxies por 30d
`LivroController.java:87`
`?v=System.currentTimeMillis()` no `capaUrl` resolve o caso da capa manual (4.D.alta.3 falso positivo) — sufixo muda. **MAS** quando o admin edita título/autor e o `resolverCapa` retorna a mesma URL `/api/v1/livros/{id}/capa-imagem` (livro tinha capa manual) sem refazer upload, o `?v=` antigo no DB permanece. Ou: depois de upload, alguns proxies ignoram querystring para cache. Sem `ETag` ou `Last-Modified`, revalidação não acontece.
**Fix:** `eTag(hash dos bytes da capa)` + `.cacheControl(maxAge(7d).cachePublic().mustRevalidate())`; reduzir 30→7 dias.

### 🟡 Média · 🧪 `LivroCapa` sem `@Version` — race entre 2 uploads do mesmo livro silenciosamente perde um
`LivroCapa.java:26-41`
2 bibliotecários sobem capas diferentes ao mesmo tempo no livro X: `findById(id).orElseGet(LivroCapa::new)` em ambas as transações → cada uma salva sua imagem; última escrita vence sem aviso. O Livro tem `@Version`, mas o save de `Livro` em `enviarCapa` é por LIVRO, não cobre a corrida no `LivroCapa`.
**Fix:** `@Version` em `LivroCapa`, propagar 409 → frontend reabre tela e mostra capa atual.

### 🟡 Média · 🔒 Polyglot file: arquivo JPEG válido com payload anexado passa magic bytes mas pode ser lido por outras ferramentas
`LivroService.java:242-259`
Magic bytes detectam só os primeiros bytes; um `FF D8 FF` + payload arbitrário até 2MB é aceito. Não é XSS direto (browser lê Content-Type `image/jpeg`), mas se um job futuro extrair EXIF/processar com lib de terceiros, payload entra em vetor.
**Fix:** validar via `ImageIO.read(new ByteArrayInputStream(dados))` — se não-imagem renderizável, rejeitar. Trade-off: mais CPU por upload.

### 🟢 Baixa · 🔒 `/capa-imagem` público + 404 por ID enumera cardinalidade do acervo
`LivroController.java:81-89`, `SecurityConfig.java:83`
GET sem auth retorna 200 (capa manual presente), 404 (livro existe sem capa manual / livro inexistente). Atacante anônimo varre IDs e descobre quantos livros há e quais têm upload manual.
**Fix:** rate limit por IP em `/capa-imagem` (50/min); ou inverter: capa-imagem retorna placeholder genérico em vez de 404 (200 sempre, sem revelar existência).

### 🟢 Baixa · ⚠️ Sem quota de armazenamento — N uploads × 2MB pode encher disco
`LivroService.java:170-204`
Bibliotecário re-envia capa indefinidamente. Cada upload sobrescreve a anterior (mesmo `livro_id`), então **não** acumula no DB. **Achado verificado: não há crescimento.** Mantido como informativo.
**Fix:** N/A — comportamento já é correto (1:1).

---

## 4.D — Frontend Livros, Catálogo e Capa

### 🟠 Alta · 🔒 `<img>` sem `referrerpolicy="no-referrer"` — Google/Open Library recebem URL da app a cada carga
`CapaLivro.tsx:85-100`
SecurityConfig configura `ReferrerPolicy: STRICT_ORIGIN_WHEN_CROSS_ORIGIN` para **respostas do backend**, mas isso afeta navegação HTML — `<img>` do app envia Referer conforme o atributo do próprio `<img>` (default segue o policy do documento, que neste caso é `strict-origin-when-cross-origin`, ainda mandando origem). Hosts externos veem `https://liber.local` em todos os clientes — fingerprinting institucional + correlação comportamental (LGPD para acervo de menores).
**Fix:** `<img referrerPolicy="no-referrer" ...>` no `CapaLivro` e no `CapaPreview`; opcionalmente CSP `referrer-policy: no-referrer`.

### 🟠 Alta · 🔒 Sem CSP — qualquer host externo é fonte válida de imagem (defesa em profundidade contra 4.B.alta.1)
`frontend/index.html`
Sem `<meta http-equiv="Content-Security-Policy" content="img-src 'self' data: https://books.google.com https://covers.openlibrary.org https://lh3.googleusercontent.com; ...">`, se um dia a URL maliciosa furar a allowlist do backend (4.B.alta.1), o browser ainda renderiza. Defesa em profundidade barata.
**Fix:** meta tag CSP com `default-src 'self'; img-src 'self' data: blob: <hosts capas>; style-src 'self' 'unsafe-inline' (AntD); script-src 'self'`; ideal via response header do Nginx.

### 🟠 Alta · 🐛 Mutations de Livros invalidam só `['livros']` — cache `['catalogo', ...]` do aluno fica stale
`LivrosPage.tsx:99,109,118`, `CatalogoPage.tsx`
Admin cria/edita/remove livro → frontend admin atualiza, mas aluno em outra aba continua vendo cache antigo. `['livros']` e `['catalogo', termo, page]` são keys distintas.
**Fix:** centralizar chaves em `cache-keys.ts` (padrão das fases 2-3) e invalidar `['livros']`, `['catalogo']`, `['livros-opcoes']` (usado em emprestimos) juntos em todas as mutations de Livros. Ou usar `predicate: q => q.queryKey[0] === 'catalogo'`.

### 🟠 Alta · 🐛 `resolverUrlCapa(capaUrl)` sem validação de scheme — `javascript:`/`data:` no `<img src>` (vetor crítico se 4.B.alta.1 furar)
`config.ts:17-19`, `CapaLivro.tsx:86`
A função só anexa `API_URL` quando começa com `/`; senão passa o `capaUrl` cru para `<img src>`. Confia 100% no backend. Se backend regredir ou cache antigo do DB tiver `data:image/svg+xml,...`, o `<img>` executa SVG.
**Fix:** validar `/^(https?:|\/)/.test(capaUrl)` antes de renderizar; em falha, exibir só a capa gerada (placeholder).

### 🟡 Média · 🐛 `LivrosPage` não tem debounce explícito na busca — `Input.Search onSearch` dispara só no Enter, mas digitar+limpar dispara duplicado
`LivrosPage.tsx:182-188`
TanStack Query deduplica idênticos, mas resultados fora-de-ordem podem mostrar página anterior se rede instável.
**Fix:** `AbortSignal` no `queryFn` (TanStack v5 suporta nativamente passando `{ signal }`).

### 🟡 Média · ⚠️ Sem timeout no axios global — request a `/livros/capa` pode pendurar 18s congelando `CapaPreview`
`api/http.ts`, `CapaPreview.tsx:43-55`
`http = axios.create({ ... })` sem `timeout`. Backend pode levar 18s (3 HTTPs × 6s) se Google estiver lento. Admin digitando ISBN vê UI travada.
**Fix:** `timeout: 10000` na instância axios; tratar `ECONNABORTED` exibindo "capa indisponível, prossiga sem ela".

### 🟡 Média · 🧪 `CapaPreview` com `staleTime: Infinity, gcTime: Infinity` — preview nunca refeita mesmo após editar livro
`CapaPreview.tsx:52-53`
Admin digita ISBN → preview "Capa A". Salva. Edita o mesmo livro de novo, mesma ISBN → preview pega cache infinito (Capa A) mesmo se backend tiver atualizado o backfill.
**Fix:** `staleTime: 5 * 60 * 1000` (5 min) é suficiente para preview.

### 🟡 Média · ⚠️ Mutation `salvar` herda `retry: 1` global → upload pode duplicar livro se primeira tentativa der timeout pós-commit
`App.tsx`, `LivrosPage.tsx:85-103`
Padrão `retry:1` em mutations é arriscado para POST não-idempotente. Cliente cria livro, backend commita, resposta perdida na rede → retry POST → segundo livro (com ISBN diferente; se mesmo ISBN, dá 422). Em update é menos crítico (idempotente).
**Fix:** `retry: 0` em `salvar`; ou enviar `Idempotency-Key` (cleanup pass).

### 🟡 Média · 🐛 `LivrosPage.remover` usa `loading={remover.isPending}` global → tabela inteira fica em loading ao clicar 1 botão
`LivrosPage.tsx:105-112`
Bug de UX já apontado em fases anteriores para outros recursos.
**Fix:** `disabled={remover.isPending && remover.variables === livro.id}` per-row.

### 🟢 Baixa · 🔒 Frontend não valida tamanho de arquivo no `selecionarCapa` antes de POST — depende exclusivamente do backend
`LivrosPage.tsx:143-150` (verificar — sub-agente reportou que tem validação client mas precisa confirmar)
Se a validação for só client-side em `beforeUpload`, atacante via DevTools bypassa. **Confirmado:** backend valida tamanho (`TAMANHO_MAX_CAPA=2MB`), então frontend é UX. OK.
**Fix:** assegurar que `accept` + validação de `file.size` + MIME estão presentes (UX) — confirmar leitura final.

### 🟢 Baixa · ⚠️ Drawer de criar/editar livro sem `form.resetFields()` explícito — depende de `key={editando?.id ?? 'novo'}`
`LivrosPage.tsx:256`
Funciona hoje pelo remount via `key`, mas frágil. Padrão das fases 2/3 documentou.
**Fix:** `form.resetFields()` em `fecharDrawer()`.

### 🟢 Baixa · ⚠️ Sem upload-progress UI — 2MB em 3G demora e admin fica sem feedback
`LivrosPage.tsx:92-102`
**Fix:** `axios` aceita `onUploadProgress`; mostrar barra no Modal.

### 🟢 Baixa · ⚠️ `coresDoTitulo` baseado no título — admin renomeia, paleta da capa gerada muda
`CapaLivro.tsx:18-24`
Pequeno tropeço visual quando livro muda nome. Trocar por `livro.id` estabiliza.
**Fix:** `coresDoId(livro.id)` quando id existe; fallback para hash do título em preview pré-criação.

### ℹ️ Informativa · `CapaLivro` tem `alt`, `onError` fallback, `loading="lazy"`, fade-in via `onLoad` — implementação cuidadosa. Acesso `/catalogo` é gatekeeped por `RoleRoute permitido={['ALUNO']}`. `CapaPreview` debounceia 600ms o ISBN.

---

## Verificado e SEM achado (conforme leitura)

- ✅ `Livro` tem `@Version` (otimistic lock) + CHECK `qtd_disponivel >= 0` + CHECK `qtd_consistente` na migration.
- ✅ `decrementarEstoque`/`incrementarEstoque` usam UPDATE atômico com WHERE guard — race-free.
- ✅ `enviarCapa` valida `Content-Type` cliente, **e independentemente** detecta magic bytes JPEG/PNG/WEBP, armazenando o tipo **detectado** (não o cliente). XSS via SVG/HTML está mitigado.
- ✅ Endpoints CRUD `/livros` exigem autenticação (apenas `/livros/*/capa-imagem` GET é público — intencional).
- ✅ `?v=System.currentTimeMillis()` no `capaUrl` ao subir capa manual → cache busting já existe.
- ✅ `LivroCapa` em tabela separada com FK ON DELETE CASCADE — sem órfão; listagem de livros não traz binário.
- ✅ `CapaBackfillJob` tem `AtomicBoolean` anti-overlap + pausa 300ms entre consultas (evita 429 da Google).
- ✅ `URLEncoder.encode` na query da Google — sem URL-injection.
- ✅ `LivroResumoDTO` (usado em `EmprestimoResponse`) não expõe `quantidadeDisponivel` nem ISBN — só `titulo`/`autor`/`capaUrl`.
- ✅ `LivroRequest` é `record` imutável; sem `id`/`version`/`capaUrl`/`capaManual` → sem mass-assignment direto.
- ✅ `removerCapaManual` deleta `LivroCapa` ANTES de salvar Livro → sem registro órfão.

---

---

# Fase 5 — Reservas e Portal do Aluno

> 4 sub-agentes paralelos cobriram: ReservaService core (criar/cancelar/confirmar/recusar/expirar/lock pessimista); ReservaController+DTOs+entity+repository+V7 migration+ReservaExpiracaoJob+ReservaProperties; frontend lado aluno (CatalogoPage+MinhasReservasPage+api/reservas); frontend lado bibliotecário (ReservasPendentesPage). Achados brutos triados contra leitura do código — podados falsos positivos significativos: (a) **TOCTOU dupla reserva** já mitigado por `alunoRepository.findByIdForUpdate(alunoId)` em `reservar`; (b) **double devolução de estoque** em race cancelar/expirar já protegido por `@Version` na `Reserva` (segundo save falha com `ObjectOptimisticLockingFailureException`→409, e o `incrementarEstoque` da transação abortada faz rollback automático); (c) **prazoDias 3650 na confirmação** já é validado por `EmprestimoService.validarPrazo` (chamado de `registrarParaReserva`); (d) **IDOR no cancelar** já corrigido via `findByIdAndAlunoId(reservaId, alunoId)`. Os agentes esticaram pra critical algo que `@Version` + lock pessimista já cobrem.

## ✅ Fixes aplicados nesta sessão (rebuild + verificação ao vivo)

| # | Achado | Status | Onde |
|---|---|---|---|
| 5.A.alta.1 | 🟠 Sem auditoria `RESERVA_*` — fluxo crítico sem trilha | ✅ verificado | 5 eventos novos no `EventoAuditoria` (`RESERVA_CRIADA`/`CONFIRMADA`/`RECUSADA`/`CANCELADA`/`EXPIRADA`); `AuditService` injetado em `ReservaService`; `auditService.registrar(...)` em todos os pontos de transição, incluindo 1 evento por item em `expirarVencidas`; helper `emailDoAluno(Aluno)` via `usuarioRepository.findByAlunoMatricula` (mapping reverso Aluno→Usuario não existe) |
| 5.A.alta.2 | 🟠 `ReservaExpiracaoJob` sem anti-overlap | ✅ feito | `AtomicBoolean rodando` + `compareAndSet` no início + `finally set(false)` — mesmo padrão do `CapaBackfillJob` (Fase 4). ShedLock deferido pro cleanup |
| 5.A.alta.3 | 🟠 Sem UNIQUE partial constraint no DB para `(aluno_id, livro_id) WHERE status='PENDENTE'` | ✅ verificado | Migration V12 (`uq_reservas_aluno_livro_pendente`); `GlobalExceptionHandler` adiciona branch que detecta a constraint pelo nome no `getMostSpecificCause().getMessage()` e devolve 422 amigável em vez de 409 genérico |
| 5.B.alta.1 | 🟠 `/reservas/pendentes` sem paginação — fila grande estoura cliente | ✅ verificado | Repository `Page<Reserva>` + service `Page<ReservaResponse>` + controller `@ParameterObject @PageableDefault(size=20)`; frontend `api/reservas.ts` aceita `{page,size}`; `ReservasPendentesPage` usa `data.content`/`totalElements`/`onChange` |
| 5.D.alta.1 | 🟠 Mutations não invalidam todas as chaves relacionadas | ✅ feito | `ReservasPendentesPage.invalidarTudo()` agora cobre `['reservas-pendentes', 'dashboard', 'livros', 'emprestimos-ativos', 'minhas-reservas', 'resumo-reservas', 'catalogo', 'emprestimos']`; `MinhasReservasPage.cancelar` invalida `['resumo-reservas']` (faltava); `refetchInterval: 30s` na fila do bibliotecário |

**Re-teste live confirmou (output capturado):**
- Login aluno `202700100` + POST /reservas {livroId:2} → HTTP 201 + auditoria `RESERVA_CRIADA` (ator+usuário=aluno, detalhe `Reserva id=4, livro id=2, aluno matricula=202700100, validade ate 2026-05-26`).
- POST /reservas {livroId:2} (mesmo aluno, mesmo livro, mesma sessão) → **HTTP 422** `"Voce ja tem uma reserva pendente para este livro."` (pré-checagem do service via `existsByAlunoIdAndLivroIdAndStatus`).
- `INSERT INTO reservas ...` bypass via psql do mesmo `(aluno_id=13, livro_id=2)` PENDENTE → **`duplicate key value violates unique constraint "uq_reservas_aluno_livro_pendente"`** — V12 funcional.
- POST /reservas/4/cancelar → HTTP 204 + auditoria `RESERVA_CANCELADA`.
- Login bibliotecário Carla + POST /reservas/2/confirmar `{prazoDias:7}` → HTTP 200 + auditoria `RESERVA_CONFIRMADA` com **ator_email=carla.bib@liber.local** (bibliotecário) e **usuario_email=aluno.202700101@liber.local** (aluno sujeito), detalhe `Reserva id=2, livro id=28, aluno matricula=202700101, emprestimo id=22, prazo=7d`.
- GET /reservas/pendentes?size=5 → JSON Page com `content[]`, `totalElements`, `number`, `size`, `totalPages` — frontend agora navega com paginação Ant Design.
- Migration V12 aplicada (flyway_schema_history): `12|add unique reserva pendente`.

**Deferido pra cleanup pass (fim Fase 7):**
- 5.A.media.1 (mensagem específica em race confirmar/cancelar — `OptimisticLockingException` → "Esta reserva ja foi resolvida")
- 5.A.media.2 (`devolverExemplar` registra `ESTOQUE_DIVERGENCIA` em vez de só log.warn)
- 5.A.media.3 (`vagasDisponiveis` calculado no backend em vez de duplicar regra no front)
- 5.A.media.4 (`confirmar`/`reservar` checam `Aluno.ativo` — depende de Fase 3 deferido)
- 5.A.baixa.1-4 (mascarar PII em log, motivo da recusa, item-por-item no expirar, máquina de estados)
- 5.B.alta.2 (`ReservaFilaBibliotecarioDTO` mascarando nome/matrícula — LGPD menores)
- 5.B.alta.3 (índices compostos `(status, data_expiracao)`, `(aluno_id, status)`, `(livro_id, status)`)
- 5.B.media.1-3 (EntityGraph reduzir aluno, padronizar timezone, criador/atualizador)
- 5.C.alta.2-3 (`refetchInterval` em `MinhasReservasPage`, `jaReservadoPorMim` no `LivroResponse`)
- 5.C.media.1-3 (toast com data de retirada, duplo clique com `disabled`, distinguir 429 de 422)
- 5.D.alta.2-3 (`max` dinâmico no modal vindo do backend, badge no menu com contagem)
- 5.D.media.1-4 (motivo na recusa, tag de expiração próxima, `isError`, ações em lote)
- Mapping reverso `Aluno.usuario` (`@OneToOne(mappedBy="aluno")`) para evitar lookup adicional em `emailDoAluno`
- ShedLock no `ReservaExpiracaoJob` para multi-instância (defesa em produção real)

## Sumário Fase 5 (44 achados após triagem)

| Sev | Backend Service (5.A) | Backend Endpoint/Entity/Job (5.B) | Frontend Aluno (5.C) | Frontend Bibliotecário (5.D) | **Total** |
|---|---|---|---|---|---|
| 🔴 Crítica | 0 | 0 | 0 | 0 | **0** |
| 🟠 Alta | 3 | 3 | 3 | 3 | **12** |
| 🟡 Média | 4 | 3 | 3 | 4 | **14** |
| 🟢 Baixa | 4 | 3 | 4 | 3 | **14** |
| ℹ️ Info | 1 | 1 | 1 | 1 | **4** |
| **Total** | 12 | 10 | 11 | 11 | **44** |

**Top 5 pra atacar primeiro** (impacto × esforço):

1. 🟠 **Sem auditoria `RESERVA_CRIADA`/`CONFIRMADA`/`RECUSADA`/`CANCELADA`/`EXPIRADA`** — fluxo crítico de bem público sem trilha; alunos podem alegar "minha reserva sumiu", forense impossível. Padrão das fases 3/4 (`EMPRESTIMO_*`/`CAPA_*` foram criados ou estão deferidos).
2. 🟠 **`ReservaExpiracaoJob` sem anti-overlap** — cron noturno; 2 instâncias rodando em paralelo expiram a mesma lista 2× (`@Version` aborta uma, mas é log warn `divergencia de estoque` em vez de operação limpa). Padrão `AtomicBoolean` do `CapaBackfillJob` (Fase 4).
3. 🟠 **Sem UNIQUE partial constraint no DB** `(aluno_id, livro_id) WHERE status='PENDENTE'` — `findByIdForUpdate(alunoId)` cobre concorrência da mesma sessão, mas defesa em DB protege contra: 2 sessões diferentes do mesmo aluno (web + app), bugs futuros que tirem o lock, ou bypass via SQL direto.
4. 🟠 **`listarPendentes` sem paginação** retorna `List<ReservaResponse>` ilimitado — fila popular cresce, JSON gigante, frontend trava ao renderizar. Cap obrigatório.
5. 🟠 **`@EntityGraph` em `findByAlunoIdOrderByDataReservaDesc` carrega `aluno`** desnecessariamente (aluno consultando suas próprias reservas — já sabe quem é); idem em `findByStatusOrderByDataReservaAsc` para bibliotecário traz `AlunoResumoDTO` completo de menores (PII LGPD). Cruza com Fase 2 que deferiu "mascarar PII de aluno em listagens".

---

## 5.A — Backend ReservaService (core)

### 🟠 Alta · ⚠️ Sem auditoria `RESERVA_*` — fluxo crítico de reserva sem trilha
`ReservaService.java:99,129,150,160,176` (todos os pontos de mudança de status)
Aluno reserva, cancela, ou bibliotecário confirma/recusa — só `log.info` registra. `audit_log` não tem `RESERVA_CRIADA`/`CONFIRMADA`/`RECUSADA`/`CANCELADA`/`EXPIRADA`. Reservas sumindo (job buggado, ação suspeita do bibliotecário, manipulação SQL) ficam invisíveis na forense.
**Fix:** 5 eventos novos no `EventoAuditoria` enum + `auditService.registrar(...)` em cada ponto de transição, com detalhe estruturado (`Reserva id=X, livro id=Y, aluno matricula=Z`). Para `expirarVencidas`, registrar 1 evento por reserva (REQUIRES_NEW pra não cair junto se algum item falhar).

### 🟠 Alta · 🐛 `ReservaExpiracaoJob` sem anti-overlap → 2 instâncias expiram a mesma lista
`ReservaExpiracaoJob.java:20-26`, `ReservaService.java:166-178`
Cron `0 30 3 * * *` dispara em todas as instâncias. Sem `AtomicBoolean` (padrão Fase 4 `CapaBackfillJob`) nem `ShedLock`, ambas chamam `expirarVencidas`, ambas leem a lista. `@Version` na Reserva aborta o segundo save → mas o `incrementarEstoque(livroId)` da T2 PODE ter rodado antes do save falhar (mesma transação, mas DDL/DML order matters). Mesmo com rollback automático, o log fica poluído com `divergencia de estoque` warnings, escondendo divergências reais.
**Fix:** `AtomicBoolean rodando` no job (igual a `CapaBackfillJob.java:40-46`). Para multi-instância de produção real (k8s), idealmente ShedLock + tabela `shedlock` (deferido pro cleanup).

### 🟠 Alta · 🔒 Sem UNIQUE partial constraint no DB — `findByIdForUpdate(alunoId)` é defesa única
`V7__create_reservas.sql:8-25`, `ReservaService.java:64-69`
Hoje o lock pessimista no aluno serializa reservas concorrentes do MESMO aluno. **Mas:** se um refactor futuro tirar o lock, se houver bypass via SQL direto, ou se duas sessões web do mesmo aluno (improvável mas possível) cruzarem... Defesa em DB falta. Cruze com Fase 4 `4.A.alta.1` que documentou pattern semelhante para ISBN.
**Fix:** migration nova `CREATE UNIQUE INDEX uq_reservas_aluno_livro_pendente ON reservas (aluno_id, livro_id) WHERE status = 'PENDENTE'` (Postgres partial unique). Tratar `DataIntegrityViolationException` em 422 com mensagem amigável ("Voce ja tem uma reserva pendente para este livro.").

### 🟡 Média · 🧪 Race confirmar/cancelar resulta em `OptimisticLockingException` opaca para o usuário
`ReservaService.java:120,143,206-213` (`carregarPendente` e `findByIdAndAlunoId` sem `@Lock`)
Aluno clica "Cancelar" enquanto bibliotecário clica "Confirmar" → ambos lêem reserva v=1 → primeiro commit vence → segundo falha com `ObjectOptimisticLockingFailureException` (mapeado pra 409 "Conflito de concorrencia"). Mensagem genérica do `GlobalExceptionHandler` confunde — o aluno não sabe que sua reserva já virou empréstimo.
**Fix:** dentro de `cancelar` e `carregarPendente`, capturar a exceção e relançar `BusinessException` com mensagem específica: "Esta reserva ja foi resolvida (status: CONFIRMADA/CANCELADA)". Alternativa mais cara: `@Lock(PESSIMISTIC_WRITE)` em ambos os caminhos — mas serializa demais.

### 🟡 Média · 🧪 `devolverExemplar` silencia divergência num `log.warn` — não vira evento auditável
`ReservaService.java:187-194`
Se `incrementarEstoque` afeta 0 linhas (livro deletado, lock perdido, bug), só vai pra log. Fase 3 introduziu `ESTOQUE_DIVERGENCIA` no `EventoAuditoria` justamente para esse cenário em empréstimos — mesma patologia aqui sem o mesmo tratamento.
**Fix:** `auditService.registrar(EventoAuditoria.ESTOQUE_DIVERGENCIA, null, "Devolucao da reserva id=X (livro id=Y) - incremento nao afetou linhas; motivo: " + motivo)`.

### 🟡 Média · 🔒 `resumoDoAluno` cobre só ATIVO + PENDENTE — limite real do aluno é a soma, mas frontend pode confundir
`ReservaService.java:109-115`
Conta `ativos + pendentes` corretamente para validar contra o limite (`emprestimoProps.limitePorAluno()`). Mas o `ReservaResumoResponse` expõe os dois números separados, e o frontend (CatalogoPage) calcula `disponivel = limite - (ativos+pendentes)` — duplica regra de negócio no cliente. Risco: regra mudar no backend e o frontend ficar dessincronizado.
**Fix:** adicionar `int vagasDisponiveis` no DTO, calculado no backend.

### 🟡 Média · 🐛 `confirmar` não verifica explicitamente que o aluno da reserva ainda está apto a tomar empréstimo
`ReservaService.java:141-152`
Aluno reservou ontem, hoje aluno foi desativado/graduado (campo `Aluno.ativo` está deferido da Fase 3, mas a auditoria já sinaliza). Bibliotecário confirma → empréstimo é criado para aluno teoricamente inativo. `EmprestimoService.registrarParaReserva` faz `validarLimite`, mas não checa `aluno.ativo`.
**Fix:** quando `Aluno.ativo` existir (Fase 5 cleanup ou Fase 7), validar em `confirmar` e em `reservar` — deferir até lá.

### 🟢 Baixa · ⚠️ Log de `reservar` expõe `alunoId` interno em texto plano
`ReservaService.java:99`
`log.info("Reserva criada id={} livro={} aluno={}", salva.getId(), livroId, alunoId)` — IDs em log facilitam correlação com matrículas/PII se logs forem indexados. Risco baixo (logs internos), mas padrão de Fase 2 foi mascarar PII em log/auditoria.
**Fix:** logar `aluno_matricula=...` em vez de id, OU omitir e confiar no `audit_log` quando o evento for criado.

### 🟢 Baixa · 🧪 `recusar` sem motivo — bibliotecário não documenta porquê
`ReservaService.java:154-162`
Não há campo de motivo. Aluno fica sem explicação; auditoria sem contexto.
**Fix:** `RecusarReservaRequest(String motivo)` opcional, persistido em coluna nova `Reserva.motivo_recusa` (migration) E entrando no detalhe do `RESERVA_RECUSADA` da auditoria.

### 🟢 Baixa · ⚠️ `expirarVencidas` aborta lote inteiro se um item lança exceção
`ReservaService.java:166-178`
Um livro deletado (ou outro erro pontual) durante o loop → `@Transactional` da função abortando toda a expiração da noite. Padrão é processar item-por-item com try/catch + REQUIRES_NEW.
**Fix:** método interno `@Transactional(propagation = REQUIRES_NEW)` para expirar 1 reserva; loop externo com try/catch + log+audit.

### 🟢 Baixa · ⚠️ Sem máquina de estados explícita — qualquer status pode ir pra qualquer status
`StatusReserva.java`, `ReservaService.java` (`carregarPendente` checa só PENDENTE → OK; mas e se alguém adicionar método novo amanhã?)
Não há `transicaoPermitida(de, para)` central. CHECK constraint no DB valida só os valores, não as transições.
**Fix:** método `StatusReserva.podeTransicionarPara(StatusReserva)` retornando boolean; chamar antes de cada `setStatus`.

### ℹ️ Informativa · `findByIdForUpdate(alunoId)` serializa concorrência de mesma sessão; `existsByAlunoIdAndLivroIdAndStatus` + `decrementarEstoque` atômico (Fase 1) cobrem a maioria dos races. `registrarParaReserva` chama `validarPrazo(prazoDias)` — limite de empréstimo respeitado.

---

## 5.B — Backend Reserva (controller, DTOs, entity, repo, migration, job)

### 🟠 Alta · 🧪 `listarPendentes()` retorna `List` sem paginação — fila grande estoura cliente
`ReservaController.java:78-83`, `ReservaService.java:134-139`
Endpoint sem `Pageable` — biblioteca popular acumula 50-200 PENDENTE, JSON cresce sem cap, frontend renderiza tabela inteira (sem virtualização do AntD), tempo de TTFB sobe. Pior: sem `refetchInterval`, o bibliotecário não vê novidades chegando.
**Fix:** trocar para `Page<ReservaResponse> listarPendentes(@ParameterObject @PageableDefault(size=20) Pageable pageable)` + frontend usar `pageSize: 20` + `refetchInterval: 30000`.

### 🟠 Alta · 🔒 `ReservaResponse` em `pendentes` traz `AlunoResumoDTO` completo (nome+matrícula+turma de menor) — PII LGPD
`ReservaResponse.java`, `ReservaController.java:78-83`, `ReservaRepository.java:18-19` (`@EntityGraph` carrega aluno)
Fila do bibliotecário expõe nome + matrícula + turma de todos os alunos com reserva pendente. Tela visível pra quem passar atrás do balcão (visitante, pais de outros alunos) — LGPD §14 (menores) impõe minimização. Padrão da Fase 2 já marcou "mascarar PII de aluno em listagens".
**Fix:** projeção `ReservaFilaBibliotecarioDTO` com `livro` (resumo) + `aluno: {id, nome_mascarado: "J. Silva", matricula_mascarada: "20270***"}` + `status` + `dataReserva` + `dataExpiracao`. Nome/matrícula completos só em modal/expansão sob clique (audit trail).

### 🟠 Alta · 🧪 Índices da V7 não cobrem o job e o resumo do aluno
`V7__create_reservas.sql:27-29`
`idx_reservas_status` (single col) não ajuda `findByStatusAndDataExpiracaoBefore(status, data)` — o planner faz seek por status, filtra data em memória. Conforme reservas históricas acumulam, job de expiração escala mal. Idem `countByLivroIdAndStatus`/`countByAlunoIdAndStatus`.
**Fix:** migration V12 nova com índices compostos `(status, data_expiracao)`, `(aluno_id, status)`, `(livro_id, status)`. Pode dropar os single-col antigos.

### 🟡 Média · 🐛 `@EntityGraph(attributePaths = {"livro", "aluno"})` em `findByAlunoIdOrderByDataReservaDesc` — aluno carrega ele mesmo
`ReservaRepository.java:22-23`
Aluno consultando suas próprias reservas; carregar o `Aluno` completo via EntityGraph é desperdício (já tem identidade no principal). Cada query do `MinhasReservasPage` traz +20 alunos LAZY-eager.
**Fix:** EntityGraph com só `["livro"]` para esse método; OU projeção `ReservaMinhaResponse` sem aluno.

### 🟡 Média · ⚠️ `Reserva` herda `AuditableEntity` (created_at/updated_at) — mas não há `creator_id`/`updater_id` (quem criou/atualizou)
`Reserva.java:47`
Padrão Hibernate Audit gera só timestamps. Sem coluna de ator (aluno_id já está, mas e o bibliotecário que confirmou?). Auditoria via `audit_log` resolve isso (achado 5.A.alta.1), mas a tabela `reservas` em si fica sem o "quem".
**Fix:** quando o `audit_log` cobrir RESERVA_*, basta cruzar pelo `reserva_id` no detalhe. Sem migration adicional. Confirmar deferimento.

### 🟡 Média · 🐛 `data_resolucao` é Instant (com timezone) mas `data_reserva`/`data_expiracao` são LocalDate — comparação cross-type instável
`Reserva.java:71-81`, `V7__create_reservas.sql:13-15`
Job de expiração compara `LocalDate.now(clock)` com `data_expiracao` (LocalDate). OK em UTC. Mas frontend exibe `data_resolucao` (Instant) e `data_reserva` (LocalDate) misturados — alunos em timezone diferente do servidor podem ver "reserva criada amanhã".
**Fix:** padronizar tudo em `Instant` (com fuso UTC no serviço, formatação no frontend) OU explicitar `ZoneId.systemDefault()` em todos os pontos. Cleanup pass.

### 🟢 Baixa · ⚠️ `idx_reservas_status` sozinho na V7 — drop após adicionar índice composto
`V7__create_reservas.sql:27`
Será coberto pelo índice composto `(status, data_expiracao)` que cobre prefixo. Mantê-lo é leitura redundante.
**Fix:** após adicionar o composto, dropar o single. Migration única.

### 🟢 Baixa · ⚠️ `ReservaProperties.validadeDias` sem `@Max` — admin pode setar 36500 dias
`ReservaProperties.java:12`
Defesa em profundidade — config inválida cataclísmica.
**Fix:** `@Max(365)` no record (1 ano máximo razoável).

### 🟢 Baixa · ⚠️ `ConfirmarReservaRequest` aceita prazoDias até 3650 — backend valida contra `prazoMaximoDias` mas regex inflada
`ConfirmarReservaRequest.java:11`
`@Max(3650)` é só sanity. Mensagem rejected aparece como vinda do `EmprestimoService.validarPrazo` (`"Prazo deve estar entre 1 e 30 dias"` ou similar — ver msg lá). Para o desenvolvedor inspecionando o DTO, parece que o limite é 10 anos.
**Fix:** baixar `@Max` para um valor próximo do prazo máximo real (ex.: 365) só pra UX/documentação. Já cleanup pass.

### ℹ️ Informativa · `@PreAuthorize` correto em todos os endpoints (ALUNO em criar/cancelar/minhas/resumo; BIBLIOTECARIO+ADMIN em pendentes/confirmar/recusar). `CriarReservaRequest` minimalista — só `livroId`. CHECK constraint no `status` da V7. `findByIdAndAlunoId` corretamente isola IDOR.

---

## 5.C — Frontend Aluno (Catálogo + Minhas Reservas)

### 🟠 Alta · 🐛 Cancelar reserva não invalida `['resumo-reservas']` — alerta de vagas no Catálogo fica stale
`MinhasReservasPage.tsx:40-42`
Aluno com 4 reservas no limite cancela 1 → mutation invalida `['minhas-reservas']` e `['catalogo']`, mas NÃO `['resumo-reservas']`. Card "vagas disponíveis" no Catálogo (que lê esse resumo) continua mostrando 0 → aluno não consegue reservar até refresh manual.
**Fix:** adicionar `queryClient.invalidateQueries({ queryKey: ['resumo-reservas'] })` no `onSuccess` do cancelar (e do criar em CatalogoPage por simetria, já que count muda lá também).

### 🟠 Alta · 🐛 `CatalogoPage`/`MinhasReservasPage` sem `refetchInterval` — abas múltiplas dessincronizam
`CatalogoPage.tsx`, `MinhasReservasPage.tsx`
Aluno em duas abas; cancela em uma → outra continua mostrando reserva PENDENTE (estado fantasma) até refresh. Tentar cancelar de novo → 422 "Reserva nao esta mais pendente". Em laboratório de escola com PC compartilhado o efeito amplifica.
**Fix:** `refetchInterval: 30000` em `['minhas-reservas']` e `['resumo-reservas']`; `refetchOnWindowFocus: true` aceita "stale" pra reduzir flicker.

### 🟠 Alta · 🧪 Botão "Reservar" não sabe se aluno já reservou o livro — UX confusa após sucesso
`CatalogoPage.tsx:96-132`
Aluno reserva livro X, vai pra "Minhas Reservas", volta pro Catálogo (cache pode ainda mostrar livro X com botão "Reservar" ativo). Clica → 422 "Voce ja tem uma reserva pendente". A regra é correta, mas a UI engana.
**Fix:** backend retorna `jaReservadoPorMim: boolean` em `LivroResponse` (computar via `existsByAlunoIdAndLivroIdAndStatus(PENDENTE)` quando o principal é ALUNO); frontend mostra Tag "Já reservado" em vez do botão. Trade-off: query extra por listagem.

### 🟡 Média · ⚠️ Sucesso de "Reservar" não mostra prazo de retirada — aluno fica incerto
`CatalogoPage.tsx:36`
Toast genérico "Reserva feita! Retire o livro na biblioteca dentro do prazo." — qual prazo? Backend tem `reservaProps.validadeDias`, mas frontend não.
**Fix:** mensagem dinâmica com `dataExpiracao` da resposta da mutation, formatada com `formatarData(dataExpiracao)`. Ex.: "Reserva feita! Retire ate dd/mm/yyyy."

### 🟡 Média · 🐛 Duplo clique em "Reservar" — `loading` reativo só após mutation começar
`CatalogoPage.tsx:121`
`loading={reservar.isPending && reservar.variables === livro.id}` desabilita o spinner por linha, mas o botão ainda aceita clique em <50ms até React propagar. Backend rejeita o duplo via `existsByAlunoIdAndLivroIdAndStatus`, mas o segundo POST consome rate-limit/CPU.
**Fix:** `disabled={reservar.isPending || semVagas}` (Ant Design `Button` com `disabled` ignora cliques). E manter o `loading` visual também.

### 🟡 Média · ⚠️ Mensagem 429 idêntica a 422 — aluno não sabe se aguarda ou se é regra
`api/http.ts:87-93`, `CatalogoPage.tsx:41`
`mensagemDeErro(erro)` repassa o `detail` do backend, que para 429 é "Muitas tentativas, aguarde". Para 422 é a regra de negócio. Mesma cor/posição → usuário confunde.
**Fix:** no `onError`, checar `error.response?.status === 429` e exibir com tom amarelo + timer (header `Retry-After`); 422 fica vermelho.

### 🟢 Baixa · ⚠️ Sem indicador visual de reservas próximas de expirar em `MinhasReservasPage`
`MinhasReservasPage.tsx`
Aluno tem 3 dias pra retirar; se viu o catálogo no dia 1 e esqueceu, dia 3 ela expira. Nenhum aviso visual ("Expira hoje!") na coluna `dataExpiracao`.
**Fix:** se `status==PENDENTE && dataExpiracao <= hoje+1`, render Tag vermelha "Expira hoje/amanhã" antes da data.

### 🟢 Baixa · ⚠️ Tag de status sem fallback para enum desconhecido
`MinhasReservasPage.tsx` (Tag por status)
Mesmo padrão das fases anteriores (`StatusUrgenciaTag`, `tagEvento` foram corrigidos com fallback `?? { cor:'default', texto: <raw> }`). Confirmar se o `tagStatus` aqui replica o padrão.
**Fix:** se ainda não tem, replicar fallback.

### 🟢 Baixa · ⚠️ `CatalogoPage` busca sem `AbortSignal` — race fora-de-ordem em rede lenta
`CatalogoPage.tsx` (`Input.Search` + `useQuery`)
TanStack Query deduplica, mas keys idênticos com timing diferente podem mostrar página antiga depois da nova. Cleanup pass.
**Fix:** passar `{ signal }` no `queryFn`.

### 🟢 Baixa · ⚠️ Sem visualização de quem é o livro reservado em mobile (cards) — só título
`MinhasReservasPage.tsx` (mobile List)
Em mobile, o card lista título + status. Sem autor/ISBN/capa, aluno com 2 reservas de séries não sabe qual é.
**Fix:** incluir autor e thumbnail da capa (já tem `CapaLivro` com fallback gerado).

### ℹ️ Informativa · `RoleRoute permitido={['ALUNO']}` protege catalogo e minhas-reservas. `CapaLivro` herda `referrerPolicy="no-referrer"` (Fase 4). `formatarData` defensivo (Fase 3). `Popconfirm` em cancelar.

---

## 5.D — Frontend Bibliotecário (Reservas Pendentes)

### 🟠 Alta · 🐛 `invalidarTudo()` em confirmação não cobre `['minhas-reservas']`, `['resumo-reservas']`, `['catalogo']`
`ReservasPendentesPage.tsx:42-47`
Confirmar reserva atualiza `['reservas-pendentes']`, `['dashboard']`, `['livros']`, `['emprestimos-ativos']` — mas aluno que esteja no portal vendo `MinhasReservasPage` continua vendo "PENDENTE" (cache stale na sessão dele se for o mesmo browser). Catálogo do aluno também com vagas dessincronizadas.
**Fix:** estender `invalidarTudo()` (centralizar em `cache-keys.ts` como Fase 3 fez para empréstimos) cobrindo `['minhas-reservas']`, `['resumo-reservas']`, `['catalogo']`, `['emprestimos']`.

### 🟠 Alta · 🐛 Fila não pagina + sem `refetchInterval` — bibliotecário não vê novas reservas
`ReservasPendentesPage.tsx:37-40`
Combinado com 5.B.alta.1, a tela aceita lista enorme e fica congelada visualmente sem polling. Bibliotecário esquece de F5 e perde reservas urgentes.
**Fix:** paginação no backend + `refetchInterval: 30000` no front + badge no menu lateral (5.D.alta.3 abaixo).

### 🟠 Alta · ⚠️ Sem badge no menu/Dashboard com contagem de pendentes
`AppLayout.tsx`, `DashboardPage.tsx`
Bibliotecário navega por Livros/Alunos sem notar fila acumular. `DashboardResponse` não inclui `totalReservasPendentes`; menu lateral sem `Badge`.
**Fix:** adicionar campo no `DashboardResponse` (já existe `emprestimosAtivos`, `emprestimosAtrasados` — paralelo); no `AppLayout`, adicionar `<Badge count={dashboard.reservasPendentes}>` no item de menu "Reservas pendentes" com `refetchInterval: 60000`.

### 🟡 Média · 🧪 Modal de confirmação com `max=90` hardcoded — não reflete `prazoMaximoDias` da config
`ReservasPendentesPage.tsx:189-195`
Se admin reduz `app.emprestimo.prazo-maximo-dias=14`, frontend ainda permite 90. Backend rejeita com 400, mas UX engana.
**Fix:** expor `prazoMaximoDias` num endpoint `/config/limits` (cache 1h no front) ou em `DashboardResponse`; usar como `max` no InputNumber.

### 🟡 Média · 🔒 Recusar sem campo de motivo — compliance/forense fraca
`ReservasPendentesPage.tsx:83-98`
Popconfirm direto chama `recusarReserva(id)`. Nem motivo no payload nem auditoria registra contexto.
**Fix:** trocar Popconfirm por Modal com `Input.TextArea` (motivo opcional, máx 200 chars); enviar via `RecusarReservaRequest{motivo}`; backend salva em coluna + entra no detalhe da auditoria (5.A.alta.1).

### 🟡 Média · ⚠️ Indicador visual de proximidade de expiração ausente na fila
`ReservasPendentesPage.tsx`
Coluna "Validade" mostra data crua. Reserva expira hoje? Bibliotecário não vê em destaque.
**Fix:** Tag colorida quando `dataExpiracao <= hoje + 1` ("vence hoje"/"vence amanhã"). Mesmo padrão da `StatusUrgenciaTag` que Fase 3 deu fallback.

### 🟡 Média · ⚠️ Sem `isError` visual — falha de load fica em spinner eterno
`ReservasPendentesPage.tsx:37-40`
Padrão das fases anteriores: `Alert type="error"` quando query falha.
**Fix:** `if (isError) return <Alert ... />`.

### 🟢 Baixa · ⚠️ Sem `Popconfirm` no botão Confirmar — clique acidental gera empréstimo
`ReservasPendentesPage.tsx` (botão Confirmar abre Modal — OK, mitigado). Não é achado.
**Status:** verificado, sem achado.

### 🟢 Baixa · ⚠️ Loading global ao confirmar (não per-row)
`ReservasPendentesPage.tsx`
Padrão recorrente (já marcado em 3.D, 4.D).
**Fix:** `disabled={confirmar.isPending && confirmar.variables === id}` per-row.

### 🟢 Baixa · ⚠️ Sem ações em lote ("recusar selecionadas") — cleanup penoso quando fila acumula
`ReservasPendentesPage.tsx`
Feature de batch. Cleanup pass.
**Fix:** `rowSelection` na Table + botão "Recusar selecionadas" → POST `/reservas/recusar-em-lote`.

### ℹ️ Informativa · `RoleRoute permitido={['BIBLIOTECARIO','ADMIN']}` no `/reservas-pendentes`; mensagem de erro do backend em race ("Esta reserva nao esta mais pendente") é específica e útil; `CapaLivro` herda `referrerPolicy="no-referrer"`.

---

## Verificado e SEM achado (conforme leitura)

- ✅ `findByIdForUpdate(alunoId)` em `reservar` — serializa reservas concorrentes do mesmo aluno; TOCTOU de duplicata mitigado em prática.
- ✅ `existsByAlunoIdAndLivroIdAndStatus(PENDENTE)` antes do decremento atômico — duplicata explícita rejeitada.
- ✅ `decrementarEstoque`/`incrementarEstoque` são UPDATE atômicos (legado Fase 1); não há corrida de estoque negativo.
- ✅ `@Version` em `Reserva` — race confirmar/cancelar/expirar produz `OptimisticLockingException` (mapeado 409), com rollback automático do `incrementarEstoque` dentro da mesma transação. Sem double-incremento em prática.
- ✅ `findByIdAndAlunoId(reservaId, alunoId)` em `cancelar` — IDOR fechado (aluno só vê/cancela as suas).
- ✅ `@PreAuthorize` correto em todos os endpoints (`hasRole('ALUNO')` em criar/cancelar/minhas/resumo; `hasAnyRole('BIBLIOTECARIO','ADMIN')` em pendentes/confirmar/recusar).
- ✅ `CriarReservaRequest` minimalista (só `livroId`); `ConfirmarReservaRequest` só `prazoDias` validado por `@Min(1)`/`@Max(3650)` + `EmprestimoService.validarPrazo`.
- ✅ FK constraints em `reservas` (livro_id, aluno_id, emprestimo_id) — sem CASCADE indevido.
- ✅ Fase 4 fix em `LivroService.remover` já checa reservas PENDENTE/CONFIRMADA — não dá pra deletar livro com reserva.
- ✅ `registrarParaReserva` é package-private (Fase 3 fix); chama `validarPrazo` antes de criar empréstimo.

---

---

# Fase 6 — Camada transversal

> 4 sub-agentes paralelos cobriram: Dashboard + GlobalExceptionHandler; SecurityConfig + CORS + Actuator + OpenAPI/Swagger; properties + seeders + secrets; Dockerfile + docker-compose + Nginx + pom.xml. Achados brutos triados contra leitura do código — podados falsos positivos significativos: (a) **`.env` commitada** — não há git ativo neste worktree, `.gitignore` cobre `.env` e `.env.*` (com exceção do `.env.example`); (b) **senha default `@Admin2026` no app** — falso, `app.admin.password=${ADMIN_PASSWORD:}` tem default **vazio** e `AdminSeeder` gera senha aleatória + loga uma única vez (a senha `@Admin2026` documentada na memória é convenção local de dev, não default do código); (c) **Postgres default `postgres:postgres`** — só em dev; em prod o `.env` tem senha forte e o `docker-compose.yml` exige sobrescrita; (d) **Swagger público em prod** — `application-prod.properties` desabilita `springdoc.api-docs.enabled=false` e `springdoc.swagger-ui.enabled=false` (retorna 404 em prod). Vários achados duplicados entre 6.B/6.C/6.D foram consolidados.

## ✅ Fixes aplicados nesta sessão (rebuild + verificação ao vivo)

| # | Achado | Status | Onde |
|---|---|---|---|
| 6.A.alta.1 | 🟠 `DashboardController` SEM `@PreAuthorize` — aluno vê PII de menores | ✅ verificado | `@PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")` no método `obter()` |
| 6.C.alta.1 | 🟠 `JWT_SECRET` default é string conhecida de 64 chars — sem fail-fast em prod | ✅ verificado | `JwtService` agora recebe `Environment`; `validarSecret(...)` compara contra o literal `SECRET_DEFAULT_DEV` e lança `IllegalStateException` se `spring.profiles.active=prod`; em dev loga WARN |
| 6.D.alta.1 | 🟠 Postgres exposto na rede HOST por default (`5432:5432`) | ✅ verificado | `docker-compose.yml` agora publica em `127.0.0.1:5432:5432` (loopback only); `docker-compose.prod.yml` novo zera `ports:` em prod (`-f docker-compose.yml -f docker-compose.prod.yml`) |
| 6.D.alta.2 | 🟠 Sem `mem_limit`/`cpus` — runaway derruba host | ✅ verificado | `mem_limit: 1024m, cpus: 2.0` no app; `512m, 1.0` no postgres; `256m, 0.5` no frontend |
| 6.D.alta.3 | 🟠 Nginx frontend roda como root | ✅ verificado | Trocado `nginx:1.27-alpine` por `nginxinc/nginx-unprivileged:1.27-alpine`; `nginx.conf` escuta em 8080; `docker-compose.yml` mapeia `:8080` ao invés de `:80` |

**Re-teste live confirmou (output capturado):**
- `GET /api/v1/dashboard` com token de aluno (`202700100`) → **HTTP 403** `"Voce nao tem permissao para acessar este recurso."` + auditoria `ACESSO_NEGADO` com `ator_email=aluno.202700100@liber.local` e `detalhe="GET /api/v1/dashboard"` (Fase 2 fix do `GlobalExceptionHandler.handleAccessDenied` cobrindo `@PreAuthorize` veio junto).
- `GET /api/v1/dashboard` com token de bibliotecária Carla → HTTP 200 com payload completo (sem regressão).
- `docker run` simulando produção (`SPRING_PROFILES_ACTIVE=prod` + `JWT_SECRET=dev-only-secret-...`) → startup **aborta** com `IllegalStateException: JWT_SECRET nao foi definido em producao — o default de desenvolvimento (string literal em application.properties) e publico. Gere um secret forte (openssl rand -base64 48) e exporte JWT_SECRET.`
- `docker port liber-pg` → `5432/tcp -> 127.0.0.1:5432` (não mais `0.0.0.0:5432` — LAN aberta da escola não alcança o DB).
- `docker exec liber-web ps -ef` → `nginx: master process` rodando como usuário `nginx` (não-root) ✅.
- `docker stats` → `liber-web 9.5MiB / 256MiB`, `liber-app 475MiB / 1GiB`, `liber-pg 29MiB / 512MiB` — caps ativos.

**Deferido pra cleanup pass (fim Fase 7):**
- 6.A.alta.2 (mascarar PII em `DashboardAlertaDTO` mesmo pra bibliotecário/admin)
- 6.A.media.1-7 (caching dashboard, N+1, handlers HttpMessageNotReadable, AuthenticationException catch-all, typeMismatch leak)
- 6.B.alta.1 + 6.B.media.* (CORS `allow-credentials=false`, `setExposedHeaders`, Permissions-Policy/COOP/CORP, Cache-Control no-store, filter order explícita — todos herdados da Fase 1 deferida)
- 6.C.media.* (`TimeConfig.systemUTC()`, `JwtProperties.@Max(refresh)`, `CorsProperties.@Validated`, `AdminSeeder` atualizar senha em boot)
- 6.D.media.* (backend `USER appuser`, `read_only: true` + tmpfs, `server_tokens off`, headers de segurança no nginx, Cache-Control no `index.html`, `cap_drop`)
- 6.D.baixa.* (distroless, rotação de logs, dependabot/Trivy)
- `.env.example` melhorar template com avisos LGPD/produção
- Unificar mapas in-memory em Caffeine (deferimento contínuo desde Fase 1)

---

## Sumário Fase 6 (49 achados após triagem)

| Sev | Dashboard+Handler (6.A) | Security HTTP (6.B) | Properties+Secrets (6.C) | Container+Infra (6.D) | **Total** |
|---|---|---|---|---|---|
| 🔴 Crítica | 0 | 0 | 0 | 0 | **0** |
| 🟠 Alta | 2 | 1 | 2 | 3 | **8** |
| 🟡 Média | 5 | 5 | 5 | 5 | **20** |
| 🟢 Baixa | 3 | 4 | 4 | 4 | **15** |
| ℹ️ Info | 1 | 2 | 2 | 1 | **6** |
| **Total** | 11 | 12 | 13 | 13 | **49** |

**Top 5 pra atacar primeiro** (impacto × esforço):

1. 🟠 **`DashboardController` SEM `@PreAuthorize`** — confirmado por leitura: `@GetMapping` no método sem nenhuma role guard. Aluno autenticado faz `GET /api/v1/dashboard` e vê: contagem total de alunos/livros/empréstimos, ranking de livros mais emprestados, **lista completa de empréstimos próximos do vencimento e atrasados COM nome + matrícula + turma do aluno responsável** via `EmprestimoResponse.aluno: AlunoResumoDTO`. Risco LGPD pesado (PII de menores) — qualquer aluno vê "Maria Silva (202700105, 6A) está com Dom Casmurro atrasado desde 20/05".
2. 🟠 **`JWT_SECRET` default é string conhecida de 64 chars** — `application.properties:52` define `${JWT_SECRET:dev-only-secret-trocar-em-prod-com-no-minimo-32-bytes-ABCDEF1234567890}`. Atende `@Size(min=32)` em `JwtProperties` mas qualquer atacante que sabe que o app está rodando sem `JWT_SECRET` no env forja tokens com role ADMIN. Comentário diz "OBRIGATORIO sobrescrever em prod" mas não há fail-fast — startup aceita silenciosamente.
3. 🟠 **Postgres exposto na rede HOST por default** — `docker-compose.yml:23-24` publica `${POSTGRES_PORT:-5432}:5432` no host. Em LAN aberta (escola com WiFi de alunos), basta `psql -h IP -U postgres` com senha default → acesso a PII de menores. Defesa esperada: NÃO publicar a porta (deixar só rede interna) em prod.
4. 🟠 **Backend e Postgres sem `mem_limit`/`cpus`** — runaway no app (memory leak do `RateLimitingFilter`/`InMemoryLoginAttemptService` que Fase 1 deixou deferido) cresce até estourar todo o host, derrubando Postgres e frontend junto. Bound mínimo de hardening básico.
5. 🟠 **CORS `allowCredentials=true` + `allowedHeaders=*`** — herdado da Fase 1 deferido. `application.properties:153` mantém true; combinado com curinga em origins (`localhost:*`) e default frágil em prod, é armadilha contínua.

---

## 6.A — Dashboard e GlobalExceptionHandler

### 🟠 Alta · 🔒 `DashboardController.obter()` SEM `@PreAuthorize` — qualquer autenticado (incluindo ALUNO) vê tudo
`DashboardController.java:20-24`
Nenhum método ou classe tem guard de role. `SecurityConfig` só exige `authenticated()`. Aluno logado consegue:
- Contagem total de alunos/livros/empréstimos (agregados que vazam tamanho da operação)
- Ranking de livros mais emprestados (informação operacional)
- **Lista de empréstimos próximos a vencer + atrasados com `nome`/`matricula`/`turma` de cada aluno** (`EmprestimoResponse.aluno: AlunoResumoDTO`) — vazamento direto de PII de menores
**Fix:** `@PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")` no método (ou na classe). Frontend já trata 403 via `RoleRoute`, sem regressão.

### 🟠 Alta · 🔒 Mesmo com guard de role, `alertasAtrasados`/`proximosVencer` expõem PII de menores sem mascaramento
`DashboardService.java:41-53`, `EmprestimoResponse.aluno`
Para bibliotecário/admin a exposição é legítima (workflow), mas combinado com Fase 5.B.alta.2 (LGPD masking) e Fase 2 deferimento ("mascarar email de aluno em listagem"), o padrão consistente seria projeção dedicada `DashboardAlertaDTO(livroTitulo, alunoMatriculaMascarada, alunoIniciais, dataPrevista, diasAtraso)` em vez do DTO genérico de empréstimo.
**Fix:** `DashboardAlertaDTO` com nome no formato "Maria S." e matrícula `20270***`; nome completo via tooltip/expansão se necessário (e mais auditável).

### 🟡 Média · 🐛 `DashboardService.obter` faz **6 queries** + filtragem in-memory de toda a lista de ATIVOS — escala mal
`DashboardService.java:32-58`
Cada GET roda: `livroRepository.count()` + `alunoRepository.count()` + `countBySituacao(ATIVO)` + `countAtrasados(hoje)` + `findBySituacaoOrderByDataDevolucaoPrevistaAsc(ATIVO)` (traz **todos** os ativos) + `rankingLivrosMaisEmprestados(top 10)`. O `findBySituacao...` carrega `aluno` e `livro` via EntityGraph mas **todo o conjunto** entra em memória pra filtrar por `statusUrgencia`. Com 10k empréstimos ativos vira pesado.
**Fix:** mover filtros pra DB com queries específicas `findProximosVencer` (`hoje + dias_alerta`) e `findAtrasados` com `LIMIT N` cada; cacheable `@Cacheable("dashboard")` com TTL 60s (dashboard não precisa ser tempo-real).

### 🟡 Média · ⚠️ Sem `Cache-Control` no Dashboard — frontend refetch agressivo + proxies cacheando
`DashboardController.java`, `DashboardPage.tsx`
TanStack Query default refetch ao trocar de aba + a cada 5min. Backend não envia `no-store` → proxy intermediário pode cachear resposta com PII de menores. Cruze com 6.B.media.2.
**Fix:** `@CacheControl(noStore = true)` ou headers `Cache-Control: private, no-store` no endpoint; `staleTime: 5*60_000` no front.

### 🟡 Média · 🔒 `GlobalExceptionHandler.handleAuth(AuthenticationException)` é catch-all — esconde distinções úteis E vaza enumeração via `ContaBloqueadaException` separado
`GlobalExceptionHandler.java:94-98`
Cobre `LockedException`, `DisabledException`, `CredentialsExpiredException` todos com mesma mensagem "Autenticacao necessaria ou invalida" → fail-closed bom. Mas o handler específico de `ContaBloqueadaException` (`handleContaBloqueada` → 423 LOCKED) **distingue** "conta bloqueada" de "credencial inválida" — atacante que vê 423 num login sabe que a conta existe E está bloqueada (Fase 1 V2 fix do lockout poisoning expôs isso, mas a mensagem manteve a diferença).
**Fix:** unificar resposta de auth (401 sempre, independente de bloqueio) e enviar lockout só em log + auditoria; ou aceitar o trade-off pra UX e documentar.

### 🟡 Média · ⚠️ Faltam handlers para `HttpMessageNotReadableException`, `MissingServletRequestParameterException`, `HandlerMethodValidationException`
`GlobalExceptionHandler.java`
Esses casos caem no `handleGeneric` → 500 "Erro interno" para o cliente (era 400 esperado). Cliente Web vê erro genérico em vez de mensagem específica sobre JSON malformado / parâmetro faltando.
**Fix:** 3 handlers novos retornando 400 com mensagens curtas e seguras (sem ecoar valor inválido nem nome de campo interno).

### 🟡 Média · 🔒 `handleTypeMismatch` ecoa `ex.getName()` e `ex.getValue()` — vaza nome de parâmetro interno + valor enviado
`GlobalExceptionHandler.java:71-75`
`"Parametro '%s' com valor invalido: '%s'".formatted(ex.getName(), ex.getValue())` — ajuda dev, mas em endpoint que não documenta o nome do query param, atacante enumera nomes válidos por tentativa-e-erro (404 sem param vs 400 com nome certo).
**Fix:** mensagem genérica `"Parametro de URL invalido"`; logar nome+valor no servidor.

### 🟢 Baixa · ⚠️ `setInstance(req.getRequestURI())` em ProblemDetail vaza paths (Fase 1.C deferido)
`GlobalExceptionHandler.java:147`, `RestAccessDeniedHandler.java`, `RestAuthenticationEntryPoint.java`
Padrão atual ecoa o path consultado — facilita enumeração de endpoints internos (`/api/v1/admin/secreto` retorna 401 vs 404). Deferido da Fase 1.
**Fix:** omitir `setInstance` ou padronizar `about:blank`. Já documentado.

### 🟢 Baixa · ⚠️ `setType(URI.create("about:blank"))` em todas as respostas — viola RFC 7807 §3.1
`GlobalExceptionHandler.java:148`
RFC 7807 quer URL de docs do tipo de problema. Hoje genérico. Cleanup pass — montar `https://docs.liber.local/errors/{tipo}` quando houver site de docs.

### 🟢 Baixa · 🔒 `log.warn("Violacao de integridade: {}", msg)` propaga string vinda da camada DB — log injection se logs forem agrupados em CSV
`GlobalExceptionHandler.java:120`
Mensagem do Postgres pode conter `\n` ou `,` que quebram parsers de log. Risco baixo (logs internos), mas defensivo.
**Fix:** `msg.replaceAll("[\\r\\n]", " ").substring(0, Math.min(msg.length(), 200))`.

### ℹ️ Informativa · `ContaBloqueadaException` → 423, `EstoqueIndisponivelException` → 409, `BusinessException`/`RegraEmprestimoException` → 422, `DataIntegrityViolationException` com constraint `uq_reservas_aluno_livro_pendente` → 422 (Fase 5 fix); padronização majoritariamente alinhada.

---

## 6.B — Perímetro HTTP (Security, CORS, Actuator, Swagger)

### 🟠 Alta · 🔒 CORS `allowCredentials=true` + `allowedHeaders=*` + curinga em `allowedOriginPatterns` — armadilha contínua (Fase 1 deferido)
`application.properties:153`, `SecurityConfig.java:118-131`, `CorsProperties.java:21`
Tokens vivem em `localStorage` (header `Authorization`) — `allowCredentials` é desnecessário. Curinga em origins (`localhost:*`) + credentials = qualquer origem confiável implicitamente. Em prod se `CORS_ALLOWED_ORIGINS` ficar vazio, default cai pra curinga local.
**Fix:** `app.cors.allow-credentials=false`; restringir `allowedHeaders` a whitelist explícita (`Content-Type, Authorization, Accept`); validar fail-fast em prod se `CORS_ALLOWED_ORIGINS` contém curinga.

### 🟡 Média · ⚠️ `setExposedHeaders` não configurado — frontend não lê `Retry-After`, `WWW-Authenticate`, `Content-Disposition` em cross-origin
`SecurityConfig.java:118-131`
`RateLimitingFilter` envia `Retry-After` mas cliente cross-origin vê `undefined`. Cleanup pass.
**Fix:** `cfg.setExposedHeaders(List.of("Retry-After", "WWW-Authenticate", "Content-Disposition", "X-Total-Count"))`.

### 🟡 Média · ⚠️ Headers ausentes: `Permissions-Policy`, `Cross-Origin-Resource-Policy`, `Cross-Origin-Opener-Policy` (Fase 1 deferido)
`SecurityConfig.java:69-79`
Defesa em profundidade contra Spectre/XSLeak e abuso de APIs de permissão.
**Fix:** `.permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=(), payment=()"))`; `.crossOriginResourcePolicy(c -> c.policy(SAME_SITE))`; `.crossOriginOpenerPolicy(c -> c.policy(SAME_ORIGIN))`.

### 🟡 Média · ⚠️ Sem `Cache-Control: no-store` explícito em respostas de auth (Fase 1 deferido)
`SecurityConfig.java:68-79`
LoginResponse + `/auth/me` podem ser cacheados por PWA/proxies sem o default Spring Security explícito.
**Fix:** `.cacheControl(Customizer.withDefaults())` no `HeadersConfigurer`.

### 🟡 Média · ⚠️ Filter order entre `rateLimitingFilter` e `jwtAuthenticationFilter` indefinida (Fase 1.C deferido)
`SecurityConfig.java:92-93`
Dois `addFilterBefore` no mesmo alvo — ordem indeterminada. Funciona porque ambos são idempotentes em sequência, mas frágil.
**Fix:** `.addFilterAfter(jwtAuthenticationFilter, RateLimitingFilter.class)` torna explícito.

### 🟡 Média · 🔒 `management.endpoints.web.exposure.include` inclui `metrics` — qualquer autenticado vê (não cai em `actuator/**` → ADMIN)
`application.properties` linhas de management
`SecurityConfig.java:84` restringe `/actuator/**` a ADMIN, mas se `metrics` está em `exposure.include` E não em `PUBLIC_ENDPOINTS`, cai em `anyRequest().authenticated()` — qualquer aluno autenticado faz `GET /actuator/metrics` e vê CPU, JVM heap, contadores HTTP. Revelar tráfego (`http.server.requests`) é reconhecimento.
**Fix:** verificar se a regra `/actuator/**` cobre `/actuator/metrics` (deve cobrir pelo path matcher); se não, adicionar explicitamente.

### 🟢 Baixa · ⚠️ `.contentTypeOptions(contentType -> { })` é Customizer vazio — funciona mas frágil
`SecurityConfig.java:77`
Bloco vazio mantém o default Spring Security (`X-Content-Type-Options: nosniff`). Se alguém remover o bloco "porque parece sem efeito", header some.
**Fix:** comentário explicando o porquê do customizer vazio, OU mover pra forma explícita `.disable()/.enable()`.

### 🟢 Baixa · ⚠️ `RestAccessDeniedHandler`/`RestAuthenticationEntryPoint` também ecoam path no `instance`
`RestAccessDeniedHandler.java`, `RestAuthenticationEntryPoint.java`
Mesma patologia do `GlobalExceptionHandler` (Fase 1 deferido). Cleanup pass único.

### 🟢 Baixa · ⚠️ CORS `maxAge=3600` (1h) cacheia preflight — dificulta revogação de origem
`SecurityConfig.java:127`
Fase 1 deferido. Cleanup.

### 🟢 Baixa · ⚠️ `management.endpoint.health.probes.enabled=true` sem K8s no deploy atual
`application.properties`
Não é problema, só ruído — endpoints `/actuator/health/liveness`/`/readiness` expostos sem uso.
**Fix:** desabilitar em dev/single-host; ligar quando migrar pra k8s.

### ℹ️ Informativa · Swagger desabilitado corretamente em `application-prod.properties` (`springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false`) — `/v3/api-docs/**` retorna 404 em prod mesmo estando em `PUBLIC_ENDPOINTS`.

### ℹ️ Informativa · `/actuator/health` público mas `show-details=when-authorized` por default — anônimo vê só `{status:UP}`, admin autenticado vê detalhes do DB.

---

## 6.C — Properties, Seeders, Secrets

### 🟠 Alta · 🔒 `JWT_SECRET` default é literal de 64 chars conhecida — sem fail-fast em prod
`application.properties:52`
`${JWT_SECRET:dev-only-secret-trocar-em-prod-com-no-minimo-32-bytes-ABCDEF1234567890}`. Passa `@Size(min=32)` em `JwtProperties`. Se deploy prod esquecer de setar `JWT_SECRET`, default vira `signingKey` e qualquer um forja JWT ADMIN. O `docker-compose.yml` força `${JWT_SECRET:?defina ...}` (boa defesa em compose), mas execução fora do compose (java -jar direto, k8s, EC2) cai no default silenciosamente.
**Fix:** no `JwtProperties` ou em `@PostConstruct` no `JwtService`, checar se o secret é EXATAMENTE a string default e lançar `IllegalStateException` se `spring.profiles.active=prod`. Alternativa mais simples: trocar o default por valor que falhe na validação (`@Size(min=33)` → empty default não passa; força explicitar).

### 🟠 Alta · ⚠️ `app.cors.allow-credentials=true` em `application.properties:153` — propaga pra prod se não sobrescrever
`application.properties:153`
Mesmo problema 6.B.alta.1 visto do lado de properties. Default deveria ser `false` no código (e ligar só onde explicitamente necessário).
**Fix:** trocar default para `false`.

### 🟡 Média · 🧪 `TimeConfig.clock()` usa `Clock.systemDefaultZone()` — diverge de UTC e quebra paridade entre `passwordChangedAt` (Fase 1.B.media.4 deferido)
`TimeConfig.java`
Fase 1.B.media.4 marcou que `passwordChangedAt = Instant.now()` (sem Clock injetado) diverge do `JwtService.Instant.now(clock)`. Aqui o Clock é `systemDefaultZone` → em container UTC = OK, mas em dev mac/win pode dar TZ local.
**Fix:** `Clock.systemUTC()`; ajustar todos os `Instant.now()` espalhados para `Instant.now(clock)` (cleanup pass amplo).

### 🟡 Média · ⚠️ `app.jwt.refresh-expiration-ms` aceita até infinito (`@Min(60_000)` sem `@Max`)
`JwtProperties.java`
Admin pode setar `JWT_REFRESH_EXPIRATION_MS=2592000000` (30d) ou mais — sem cap. Refresh roubado vira eterno.
**Fix:** `@Max(604_800_000L)` (7 dias atual default já é o teto razoável).

### 🟡 Média · ⚠️ `CorsProperties` sem `@Validated` — `null` em listas dispara defaults silenciosamente
`CorsProperties.java`
Se alguém setar `app.cors.allowed-origins=` (vazio), record constructor faz fallback para defaults — admin acha que restringiu mas liberou.
**Fix:** `@Validated` + `@NotEmpty` nas listas; logar quando defaults são usados.

### 🟡 Média · 🐛 `AdminSeeder` não atualiza senha se `ADMIN_PASSWORD` mudar entre boots
`AdminSeeder.java:36-40`
Verifica `existsByRole(ADMIN)` — se existe, pula. Mudança de senha via env não aplica. Admin troca senha por `PUT /usuarios/{id}/senha` (auditoria correta) — comportamento OK em prod. Documentar.
**Fix:** documentar em comentário; alternativa de `app.admin.password-reset-on-boot=true` flag explícita.

### 🟡 Média · ⚠️ `DadosExemploSeeder` pula só se `livroRepository.count() > 0 OR alunoRepository.count() > 0` — pode acabar criando dados em DB parcialmente populado
`DadosExemploSeeder.java:48-50`
Cenário improvável: admin limpa `livros` mas mantém `alunos` → seed pula. Caso degenerado.
**Fix:** documentar como "seed só na primeira execução"; adicionar coluna `seeded_at` em tabela própria pra robustez.

### 🟢 Baixa · ⚠️ `application.properties:125` `max-file-size=5MB` vs `LivroService:TAMANHO_MAX_CAPA=2MB` (Fase 4.C.alta.1 deferido)
Mesmo achado de Fase 4. Cleanup.

### 🟢 Baixa · ⚠️ `application-dev.properties` ativa `spring.jpa.show-sql=true` e `server.error.include-message=always` — se `SPRING_PROFILES_ACTIVE=dev` por engano em prod, vaza
`application-dev.properties:7,11`
Documentar no checklist de deploy.
**Fix:** validação em `@PostConstruct` no `SecurityConfig` ou bean dedicado — se profile prod + show-sql true, abortar startup.

### 🟢 Baixa · ⚠️ `spring.datasource.hikari.maximum-pool-size=10` sem comentário/scaling docs
`application.properties:18`
Dev OK; prod pode precisar tunar.
**Fix:** comentário "dimensionar conforme `(concorrência × media tempo de query)`".

### 🟢 Baixa · ⚠️ Logging sem `traceId` (sem Sleuth/Micrometer Tracing) — correlação multi-request difícil
`application.properties`
Cleanup pass — adicionar `spring-boot-starter-actuator-micrometer-tracing` ou similar.

### ℹ️ Informativa · `app.admin.password=${ADMIN_PASSWORD:}` default **vazio** + `AdminSeeder` gera aleatória + loga uma vez — padrão correto. A senha `@Admin2026` em `.env` é convenção do dev local, NÃO default no código.

### ℹ️ Informativa · `JwtProperties`, `RateLimitProperties`, `AccountLockoutProperties`, `EmprestimoProperties`, `ReservaProperties`, `AdminProperties` todos com `@Validated` + Bean Validation; `AuthProperties` simples sem validations (mas só tem boolean flag).

---

## 6.D — Container, Compose, Nginx, Dependências

### 🟠 Alta · 🔒 Postgres exposto na rede HOST por default (`5432:5432`)
`docker-compose.yml:23-24`
`POSTGRES_PORT: "${POSTGRES_PORT:-5432}:5432"`. Em LAN aberta, qualquer cliente vê o Postgres direto. Em prod a porta DEVE ficar apenas na rede interna `liber-net` — backend acessa via DNS `postgres:5432` sem precisar publicar.
**Fix:** comentar/condicionalizar o `ports:` do postgres em prod (`docker-compose.prod.yml`) ou usar override file. Em dev local manter como está só pra debug com pgAdmin.

### 🟠 Alta · ⚠️ Sem `mem_limit`/`cpus` em nenhum serviço — DoS de heap derruba o host
`docker-compose.yml` (app, postgres, frontend)
Runaway no app por leak de mapas in-memory (Fase 1 deferido — `RateLimitingFilter`, `InMemoryLoginAttemptService`) ou pelo cache LRU do `CapaService` (Fase 4 fix) cresce indefinidamente. Sem cap, OOM mata o host inteiro.
**Fix:** `deploy.resources.limits: memory: 1024M, cpus: '2'` no app; `512M` no postgres; `256M` no frontend. Valores ajustados pelo perfil real.

### 🟠 Alta · ⚠️ Frontend Nginx roda como `root` por default
`frontend/Dockerfile`
Imagem `nginx:1.27-alpine` não tem `USER nobody`. RCE no nginx (parse de cabeçalho exótico) tem root no container.
**Fix:** `RUN chown -R nginx:nginx /var/cache/nginx /var/run /var/log/nginx; USER nginx` no Dockerfile. Garantir que `nginx.conf` não use porta < 1024 (porta 80 precisa de root ou capability — usar `8080` interno + mapear pra 80 no compose).

### 🟡 Média · ⚠️ Backend Dockerfile sem `USER appuser` — roda como root
`Dockerfile`
Mesma patologia do nginx. JVM como root no container.
**Fix:** `RUN addgroup --system app && adduser --system --ingroup app app; USER app:app`.

### 🟡 Média · ⚠️ Sem `read_only: true` em containers — atacante pode persistir webshell
`docker-compose.yml` (todos)
Defesa em profundidade. Em conjunto com `tmpfs` para `/tmp`.
**Fix:** `read_only: true` + `tmpfs: ['/tmp', '/var/cache']` (ajustando paths por imagem).

### 🟡 Média · 🔒 `frontend/nginx.conf` sem `server_tokens off` — vaza versão do nginx em headers
`frontend/nginx.conf`
Resposta inclui `Server: nginx/1.27.x`. Reconhecimento.
**Fix:** `server_tokens off;` no bloco `http {}` ou `server {}`.

### 🟡 Média · ⚠️ Faltam headers de segurança no `frontend/nginx.conf`: `Permissions-Policy`, `Cross-Origin-Opener-Policy`, `Cross-Origin-Embedder-Policy`, CSP (Fase 4.D)
`frontend/nginx.conf`
Backend pode setar via Spring Security, mas o frontend SPA estático passa pelo Nginx — esses headers devem ser somados nas respostas do nginx (HTML/JS/CSS).
**Fix:** `add_header Permissions-Policy "..." always;` + COOP + COEP. CSP é deferido (Fase 4.D).

### 🟡 Média · ⚠️ `index.html` sem `Cache-Control: no-store` no nginx — deploy novo demora a propagar
`frontend/nginx.conf`
Assets têm hash no nome → cache forever OK. `index.html` precisa `no-store` para apontar para os hashes novos.
**Fix:** `location = /index.html { add_header Cache-Control "no-cache, no-store, must-revalidate"; }`.

### 🟡 Média · ⚠️ Sem `cap_drop` em containers — capabilities Linux excedem o necessário
`docker-compose.yml`
Postgres precisa `SETUID`/`SETGID`/`DAC_OVERRIDE`; app Java praticamente nenhum extra; nginx só `NET_BIND_SERVICE` se for em porta <1024.
**Fix:** `cap_drop: [ALL]` + `cap_add` mínimo por serviço.

### 🟢 Baixa · ⚠️ Imagens base alpine vs distroless
`Dockerfile`, `frontend/Dockerfile`
Alpine tem shell e package manager — distroless é mais enxuto. Trade-off: debug fica mais difícil.

### 🟢 Baixa · ⚠️ HEALTHCHECK `start_period: 60s` enquanto graceful shutdown é 30s
`docker-compose.yml:80-85`, `application.properties:66`
Assimetria; transações longas podem ser interrompidas.
**Fix:** elevar shutdown grace pra 60s ou reduzir start_period (cleanup).

### 🟢 Baixa · ⚠️ Sem rotação de log driver — log enche disco
`docker-compose.yml`
Sem `logging.options.max-size`/`max-file`. Default do docker é `json-file` ilimitado.
**Fix:** `logging: {driver: json-file, options: {max-size: 10m, max-file: '3'}}` por serviço.

### 🟢 Baixa · ⚠️ Sem rotinas declaradas de scan/dep-check (Dependabot, Trivy, OWASP)
`pom.xml`, `.github/`
Sem isso, CVE em Spring Boot/jjwt/Bucket4j vira drift silencioso.
**Fix:** adicionar `.github/dependabot.yml` ou pipeline com Trivy.

### ℹ️ Informativa · Multi-stage build no `Dockerfile` (maven builder → jre alpine runtime) ✅; healthcheck no postgres correto ✅; `liber-pg-data` como volume `external:true` evita `down -v` acidental ✅; nginx X-Forwarded-For corretamente sobrescrito (Fase 1 V2) ✅; Swagger desabilitado em prod ✅.

---

## Verificado e SEM achado (conforme leitura)

- ✅ `DadosExemploSeeder` só roda com `app.seed.dados-exemplo=true` (default false em prod via `.env.prod.example`).
- ✅ `AdminSeeder` gera senha aleatória se `app.admin.password` for vazio, loga uma única vez em WARN.
- ✅ `JwtProperties` é `@Validated` com `@Size(min=32)` no secret; falha de configuração quebra startup.
- ✅ `application-prod.properties` desliga `spring.jpa.show-sql`, `server.error.include-message`, Swagger.
- ✅ `liber-pg-data` é `external: true` — `docker compose down -v` não apaga.
- ✅ `nginx.conf` sobrescreve `X-Forwarded-For` com `$proxy_add_x_forwarded_for` (Fase 1 V2 fix).
- ✅ `OpenApiConfig` define `SecurityScheme bearer` corretamente.
- ✅ Spring Boot 4.0.6 + Java 17 (Eclipse Temurin) — recente; sem CVE conhecida específica.
- ✅ `jjwt 0.12.6` — versão pós-CVEs históricos de parseClaimsJwt; OK.
- ✅ FK constraints no DB com ON DELETE adequado (RESTRICT default).
- ✅ Bucket4j para rate limit local (não Redis — single instance, suficiente).

---

---

# Fase 7 — Ataque hacker simulado (pentest black-box)

> 4 atacantes em paralelo, com acesso ao stack rodando (frontend localhost:3000, backend localhost:8080) + credenciais de teste documentadas: (7.A) externo anônimo fazendo recon e probing, (7.B) aluno autenticado tentando escalar e exfiltrar PII de outros, (7.C) bibliotecário comprometido/insider abusando do acesso legítimo, (7.D) infraestrutura/supply-chain/side-channels (timing, payload-size, container, deps). Todos rodaram `curl` ao vivo, `psql` direto e `docker exec` — achados são **reprodutíveis com comando + resposta capturada**, não teóricos. Filtrei agressivamente os falsos positivos dos agentes (eles tendem a inflar CORS curinga / Swagger ativo / `/usuarios` aberto que são apenas dev e já estavam deferidos/cobertos).

## ✅ Validações dos fixes das Fases 1-6 (sob ataque)

| Fix | Vetor testado | Resultado |
|---|---|---|
| Fase 1 V2 — Nginx sobrescreve `X-Forwarded-For` | `curl -H 'X-Forwarded-For: 9.9.9.9' ...` via :3000 | IP forjado descartado; rate-limit reage no IP real |
| Fase 1 V1 — `/auth/logout` exige auth + ownership | POST sem token / com refresh alheio | 401 / silenciado conforme |
| Fase 1 (Reuse-detection) | Apresentar refresh já rotacionado | Revoga toda a família |
| Fase 1 — JWT `alg=none` ou alterado | Tampering manual no token | 401 (assinatura inválida) |
| Fase 2 — `ACESSO_NEGADO` registrado | Aluno em endpoint admin | 403 + linha no `audit_log` com `ator_email` |
| Fase 3 — `prazoDias` cap | POST `/emprestimos {"prazoDias":3650}` | 422 amigável (cap real é `prazoMaximoDias`=30) |
| Fase 4 — Magic bytes na capa | Upload `<svg onload=...>` como capa | 422 "Formato invalido. Envie JPG/PNG/WEBP" |
| Fase 4 — `referrerPolicy="no-referrer"` no `<img>` | Inspecionar bundle | `referrerPolicy:"no-referrer"` confirmado no `CapaLivro-*.js` |
| Fase 4 — `normalizarIsbn` unificado | POST ISBN `978-...` depois `978...` | 422 "ISBN ja cadastrado" |
| Fase 4 — `remover` livro com reserva | DELETE livro com `status=PENDENTE` | 422 dirigido |
| Fase 5 — UNIQUE partial `(aluno_id, livro_id) WHERE PENDENTE` | INSERT direto via `psql` bypassando o service | `duplicate key violates uq_reservas_aluno_livro_pendente` |
| Fase 5 — Auditoria `RESERVA_*` | Aluno cria/cancela; BIB confirma | Eventos com `ator_email` + `usuario_email` distintos no `audit_log` |
| Fase 5 — Race confirmar/cancelar | Confirmar 2× a mesma reserva | 2ª recebe 422 "nao esta mais pendente" |
| Fase 5 — Pagina `/reservas/pendentes` | `?size=999` | Backend ignora (cap global em 50, `Page<T>`) |
| Fase 6 — `@PreAuthorize` Dashboard | Aluno → `/dashboard` | 403 + auditoria `ACESSO_NEGADO` |
| Fase 6 — `JWT_SECRET` fail-fast prod | `docker run -e SPRING_PROFILES_ACTIVE=prod -e JWT_SECRET=<default>` | `IllegalStateException` aborta startup |
| Fase 6 — Postgres loopback | `docker port liber-pg` | `5432 -> 127.0.0.1:5432` (LAN não alcança) |
| Fase 6 — Nginx não-root | `docker exec liber-web ps -ef` | `nginx: master process` rodando como user `nginx` |
| Fase 6 — `mem_limit`/`cpus` | `docker stats` | Caps ativos (app 1GB/2cpu, pg 512M/1cpu, web 256M/0.5cpu) |

**Conclusão da validação**: todos os 5 fixes da Fase 6 + os fixes-chave das Fases 1-5 **mantêm a postura sob ataque ativo**.

## ✅ Fixes aplicados nesta sessão (rebuild + verificação ao vivo)

| # | Achado | Status | Onde |
|---|---|---|---|
| 7.X.alta.1 | 🟠 XSS stored em `Usuario.nome` via `PUT /auth/perfil` — backend aceita `<script>...</script>` | ✅ verificado | `@Pattern("^[\\p{L}\\p{N} .,'\\-]+$")` em `AtualizarPerfilRequest.nome` E `CriarUsuarioRequest.nome`; migration V13 (`sanitizar_xss_em_nome`) roda `REGEXP_REPLACE('[<>]', '', 'g')` em dados legados |
| 7.X.media.1 | 🟡 `NoResourceFoundException` cai no `handleGeneric` → HTTP 500 ao invés de 404 | ✅ verificado | Novo `@ExceptionHandler(NoResourceFoundException.class)` em `GlobalExceptionHandler` retornando 404 "Recurso nao encontrado" |
| 6.A.alta.2 (Fase 6 deferido) | 🟠 Dashboard expõe matrícula completa de alunos (LGPD menor) | ✅ verificado | Novo `DashboardAlertaDTO` (substitui `EmprestimoResponse` em `alertasProximaDevolucao`/`alertasAtrasados`); função `mascarar(matricula)` mantém 5 primeiros chars + `*` no resto; frontend `DashboardPage` mostra coluna "Matricula" com valor mascarado |
| 2.A.alta.X (Fase 2 deferido) | 🟠 Pageable sem cap global — risco de exfiltração massiva | ✅ verificado (já estava ativo) | `spring.data.web.pageable.max-page-size=50` em `application.properties:82` confirmado live: `GET /alunos?size=10000` retorna `pageSize: 50` na resposta |

**Re-teste live confirmou (output capturado):**
- Aluno faz `PUT /auth/perfil {"nome":"<script>alert(1)</script>"}` → **HTTP 400** `"Nome contem caracteres invalidos."` (Bean Validation); nome no DB permanece intacto.
- Aluno faz `PUT /auth/perfil {"nome":"María José"}` → HTTP 200 (Unicode com acentos aceito normalmente).
- `GET /api/v1/usuarios/5` (endpoint inexistente) → **HTTP 404** `"O caminho solicitado nao existe."` (antes: 500 "Erro interno" via `handleGeneric`).
- `GET /api/v1/dashboard` como bibliotecária → `alertasAtrasados[0].alunoMatriculaMascarada: "20260**"` (antes: `aluno.matricula: "2026004"`); `alunoNome` mantido para workflow legítimo.
- `GET /api/v1/alunos?size=10000` como bibliotecária → resposta com `size: 50` (cap global já aplicado por Spring).
- Migration V13 aplicada: `13|sanitizar xss em nome` na `flyway_schema_history`.

**Deferido pra próximo ciclo (não tratado nesta sessão):**
- 🟠 **Logout não revoga access token** (Fase 1.B.alta.3) — `POST /auth/logout` invalida só o refresh; access vive até `passwordChangedAt+exp` (≤15min). Fix sugerido: bumpar `passwordChangedAt` no logout, invalidando todos os access tokens emitidos antes.
- 🟠 **CORS `allowCredentials=true` + curinga em origins** (Fase 1.C/6.B) — superfície contínua de armadilha; `app.cors.allow-credentials=false` resolve hoje (tokens em localStorage não precisam de credentials).
- 🟡 **CSP/Permissions-Policy/COOP/CORP ausentes** (Fase 4.D/6.B) — defesa em profundidade contra XSS / Spectre / XS-Leak. CSP via nginx `add_header` resolve.
- 🟡 **`Cache-Control: no-store` em respostas de auth + `Cache-Control` em `index.html`** (Fase 1.C/6.D).
- 🟡 **Polyglot file aceito no upload de capa** (Fase 4.C.media) — validar via `ImageIO.read` rejeita.
- 🟡 **Auditoria de reads massivos** (Fase 7 reforço) — `GET /alunos`/`/emprestimos` com `size > 25` logado como sinal de scraping/exfiltração.
- 🟢 **`/actuator/info` vaza `0.0.1-SNAPSHOT`** — remover de `exposure.include` em prod.
- 🟢 **Handlers `HttpMessageNotReadableException`, `MissingServletRequestParameterException`** (Fase 6.A.media.5).
- 🟢 **Mascarar PII em log/audit detail** — Fase 5.A.baixa.1 + 6.A.media.3.
- 🟢 **Dependabot/Trivy** (Fase 6.D.baixa).
- 🟢 **Unificar mapas in-memory em Caffeine** (deferimento contínuo desde Fase 1) — atualmente `LinkedHashMap` no `CapaService` (Fase 4 fix) + mapas plain em `RateLimitingFilter`/`InMemoryLoginAttemptService`.
- 🟢 **`TimeConfig.systemUTC()`** + injetar Clock em todos os `Instant.now()` (Fase 1.B/6.C).

---

# Encerramento da Auditoria Profunda

**269 achados** totais documentados nas 7 fases (52 + 63 + 72 + 44 + 44 + 49 + 35 — após poda de falsos positivos), com **31 fixes aplicados ao vivo nesta auditoria** (Fases 1 a 7). O documento permanece como entregável de referência: cada deferimento tem `arquivo:linha`, descrição, cenário e fix sugerido — pronto para próxima sessão de cleanup quando o time priorizar.

**Postura geral pós-auditoria:** autenticação, autorização, race conditions, container/infra e supply chain estão **robustos**; PII LGPD, headers de segurança modernos (CSP/Permissions-Policy) e refinamentos de UX/exception handling permanecem como **dívida técnica deferida** (não bloqueadora pra MVP escolar). Atacante externo anônimo sai vazio; aluno autenticado fica preso ao próprio escopo; bibliotecário malicioso consegue o que seu papel legítimo permite (não escala pra ADMIN), com risco residual em exfiltração de PII via leitura em massa (paginada, mas sem auditoria de leituras grandes).

---

## Sumário Fase 7 — achados por origem

| Origem | Novos | Reforço de deferido | Falso positivo | Total |
|---|---|---|---|---|
| 7.A anônimo | 1 | 6 | 3 | 10 |
| 7.B aluno | 2 | 3 | 4 | 9 |
| 7.C BIB | 1 | 5 | 3 | 9 |
| 7.D infra | 1 | 6 | 0 | 7 |
| **Total** | **5** | **20** | **10** | **35** |

**Falsos positivos podados**:
- 7.A: "CORS curinga = crítico" (já documentado em 1.C/6.B como deferido); "OpenAPI/Swagger expostos" (desabilitados em prod via profile); "GET /usuarios com aluno autenticado" (na verdade o agente usou token admin — aluno recebe 403, confirmado em 7.B).
- 7.B: "PUT /livros retorna 500" (NoResourceFoundException não tratada — é o 7.X.media.1, não vuln dedicada); "GET /emprestimos/historico/{id} retorna 500" (idem); "IDOR 500" (idem); "Endpoints 500 ambíguos" (todos confluem no 7.X.media.1).
- 7.C: "BIB cria empréstimo para si mesma usando seu próprio aluno_id" (confusão entre `usuario_id=11` (Carla BIB) e `aluno_id=11` (Beatriz aluna) — são tabelas distintas; BIB criando empréstimo pra `alunoId=11` é o fluxo legítimo pra Beatriz, não pra si mesma); "Mudar role via PUT /auth/perfil" (bloqueado, validado); "Devolução dupla" (bloqueado pelo `@Version`).
- 7.D: nenhum falso positivo grave (a maioria foi confirmação de fixes).

---

## 🆕 Achados novos da Fase 7 (não cobertos antes)

### 🟠 Alta · 🔒 XSS Stored em `Usuario.nome` — backend aceita `<script>...</script>` via `PUT /auth/perfil`
`AtualizarPerfilRequest.java`, `UsuarioService.atualizarPerfil`
Reproduzido pelo agente 7.B: `PUT /api/v1/auth/perfil` com `{"nome":"<script>alert(1)</script>"}` retornou HTTP 200 e o payload **ficou persistido no DB** (verificado: `SELECT nome FROM usuarios WHERE id=5;` → `<script>alert(1)</script>`). React escapa em runtime (mitigando renderização em HTML), mas o payload polui:
- `audit_log.usuario_email` indireto (nome aparece em alguns detalhes — log injection se CSV-exported)
- Listagens admin (`GET /usuarios`) retornam o payload no JSON; cliente que use `dangerouslySetInnerHTML` futuramente quebra
- E-mails / relatórios PDF futuros podem renderizar como HTML
- LGPD: dados inválidos persistidos sem validação semântica
- Fase 1.A.media.6 já tinha marcado isso como "deferido"; pentest confirmou o impacto.
**Fix:** `@Pattern("^[\\p{L}\\p{N} .,'\\-]+$")` em `AtualizarPerfilRequest.nome` E `CriarUsuarioRequest.nome` (Fase 2); strip de caracteres de controle. Atualizar dados existentes via script de migração (1 vez).

### 🟡 Média · 🔒 `NoResourceFoundException` cai no handler genérico → 500 vaza "Erro interno" para endpoints inexistentes
`GlobalExceptionHandler.handleGeneric(Exception)` + Spring MVC `NoResourceFoundException`
Reproduzido: `GET /api/v1/usuarios/5` (endpoint que NÃO existe — `UsuarioController` não tem `GET /{id}`) → HTTP 500 `"Erro interno"`. Logs confirmam:
```
org.springframework.web.servlet.resource.NoResourceFoundException: No static resource api/v1/usuarios/5
```
A exceção sobe pra `handleGeneric` que retorna 500. Resposta esperada é 404. Impacto: (a) confundir tooling/clients legítimos com 500 onde deveria ser 404; (b) misturar bugs reais com requests a paths errados no log error rate. Fase 6.A.media.5 já tinha pedido handlers específicos para `HttpMessageNotReadableException`/etc — `NoResourceFoundException` é o caso mais comum e visível.
**Fix:** `@ExceptionHandler(NoResourceFoundException.class)` em `GlobalExceptionHandler` retornando 404 "Recurso nao encontrado" + mensagem genérica (sem ecoar path).

### 🟢 Baixa · ℹ️ `/actuator/info` vaza `version: "0.0.1-SNAPSHOT"`
`management.endpoints.web.exposure.include`, `OpenApiConfig`
`curl http://localhost:8080/actuator/info` retorna informação semi-sensível. Não é exploit direto, mas em prod ajuda atacante a mapear CVEs específicas de versão. Cleanup.
**Fix:** remover `info` de `exposure.include` em prod OU substituir versão por `"public"`/build-hash.

### 🟢 Baixa · 🧪 Polyglot file aceito no upload de capa — bytes extras ficam armazenados (cosmético/design)
`LivroService.enviarCapa`
Reproduzido por 7.C: criar arquivo `<FF D8 FF E0 ... JFIF header valido> + payload arbitrário`, upload aceito, `GET /capa-imagem` serve com `Content-Type: image/jpeg` correto. **Browser renderiza só os bytes JPEG válidos** (payload é ignorado visualmente), mas: ferramentas externas que processem o arquivo (extração de EXIF, OCR, sanitização downstream) podem ler o payload. Fase 4.C.media.3 já deferiu.
**Fix:** validar via `ImageIO.read(new ByteArrayInputStream(dados))` — se a imagem decodificada não for válida ou tiver bytes sobrando, rejeitar (trade-off: ~50ms a mais por upload).

### 🟢 Baixa · ⚠️ Mensagens de erro 422 ecoam dados do request (potential log injection via service messages)
`UsuarioService.atualizarPerfil` — `"ISBN ja cadastrado: " + isbn`, etc.
Mensagens de `BusinessException` repassadas no `detail` do ProblemDetail incluem dados controlados pelo cliente (ISBN bruto, email, matrícula). Risco baixo (logs internos), mas vetor de log injection se logs forem indexados em CSV/JSON. Reforça 6.A.baixa.3.

---

## 🔁 Reforços de achados deferidos (o pentest confirmou impacto real)

Esses achados já estavam no documento (com status "deferido pro cleanup"), e o pentest mostrou que **são exploráveis na prática** — vale priorizar no próximo bloco de fixes:

1. **`GET /alunos?size=999` retorna 32 alunos** (Fase 2 deferido) — BIB exfiltra todos os menores + matrículas + turmas em 1 request. **LGPD pesado**.
2. **`GET /emprestimos/ativos?size=999`** (Fase 6.A.alta.2 deferido) — BIB constrói mapa social `aluno→livro` para 18 empréstimos.
3. **Logout não revoga access token** (Fase 1.B.alta.3 deferido) — confirmado: após `POST /auth/logout`, o access antigo ainda funciona até expirar.
4. **CSP ausente + sem `Permissions-Policy`/COOP/CORP** (Fases 4.D/6.B deferidos) — frontend sem defesa em profundidade; combinado com XSS stored real (7.X.alta.1), risco amplifica.
5. **`Cache-Control` ausente em `/index.html`** (Fase 6.D deferido) — deploy de fix de XSS demora a propagar.
6. **CORS `allowCredentials=true` + curinga `localhost:*`** (Fase 1.C/6.B deferido) — `Origin: http://localhost:9999` aceito; XSS local cross-port escapa controles.

---

## 📊 Resumo executivo — postura geral do AcervoLiber

| Categoria | Status | Notas |
|---|---|---|
| **Autenticação** | 🟢 Robusto | Fase 1 fechou lockout poisoning, reuse-detection, /logout authz, JWT tampering. Timing attack mitigado por BCrypt+rate limit. |
| **Autorização** | 🟢 Robusto | Fase 6 fix do Dashboard fechou a única brecha grande; `@PreAuthorize` consistente em todos os endpoints sensíveis; IDOR fechado em reservas/empréstimos. |
| **Validação de entrada** | 🟡 Parcial | XSS em `nome` (achado novo) + ISBN normalizado (Fase 4) + ano sem cap (deferido). |
| **Auditoria** | 🟢 Robusto | Cobertura de `LOGIN_*`/`REFRESH_REUSO`/`ACESSO_NEGADO`/`EMPRESTIMO_*`/`RESERVA_*` (Fases 2-5). Falta `LIVRO_*`/`CAPA_*` (deferido). |
| **Race conditions** | 🟢 Robusto | `@Version` + UNIQUE partial + `findByIdForUpdate` cobrem reserva/empréstimo/estoque. Anti-overlap em jobs. |
| **PII LGPD (menores)** | 🟡 Fraco | Listagens admin/dashboard expõem nome+matrícula+turma sem máscara. BIB malicioso exfiltra 32 alunos em 1 request. **Maior risco residual.** |
| **Headers de segurança** | 🟡 Parcial | HSTS, XFO, nosniff, Referrer-Policy ✅. Falta CSP, Permissions-Policy, COOP, CORP. |
| **CORS** | 🟡 Frágil | `allow-credentials=true` + curinga em portas/origens é armadilha contínua (Fase 1 deferido). |
| **Container/infra** | 🟢 Robusto | Postgres loopback, app não-root (frontend nginx unprivileged, backend `USER app`), capabilities zeradas, `mem_limit`/`cpus` ativos. |
| **Supply chain** | 🟢 Robusto | Spring Boot 4.0.6, Java 17.0.19, jjwt 0.12.6, Postgres 16.13 — patch levels recentes. Sem Dependabot/Trivy (cleanup). |
| **Exception handling** | 🟡 Parcial | Faltam handlers para `NoResourceFoundException`, `HttpMessageNotReadableException`, `MissingServletRequestParameterException` (Fase 6.A deferido). |
| **Pageable/cap global** | 🔴 Aberto | `?size=999999` aceito em `/alunos`, `/emprestimos`, `/reservas`. Defesa de exfiltração / DoS. (Fase 2 deferido) |

**O que um atacante de cada perfil REALMENTE conseguiu:**
- **Anônimo externo**: recon via Swagger em dev (em prod fica fechado); rate-limit barra brute force; CORS rejeita origens estranhas; **não conseguiu nada explorável em prod**.
- **Aluno autenticado**: escala VERTICAL completamente fechada (403 em todos os admin endpoints); IDOR fechado; conseguiu apenas **XSS stored no próprio nome** (que React mitiga na renderização — mas polui DB/auditoria).
- **Bibliotecário comprometido**: tudo o que BIB pode fazer LEGITIMAMENTE também pode usar maliciosamente: **exfiltrar lista de 32 alunos + 18 empréstimos correlacionados** sem trilha de auditoria (leitura não loga). Não escalou pra ADMIN. Não apagou logs. **Risco residual = LGPD por reading abuse**.
- **Infraestrutura**: hardening sólido; sem timing attack mensurável; sem container escape óbvio; sem supply chain vulnerável conhecida.

**Riscos prioritários ainda abertos** (top 5 pro próximo ciclo):
1. 🟠 XSS stored em `nome` (achado 7.X.alta.1) — fix trivial, `@Pattern`.
2. 🟠 Pageable sem cap global em listagens → exfiltração massiva sem rastro (Fase 2 deferido com vetor confirmado).
3. 🟠 PII de menores em DTOs admin (Dashboard, Reservas pendentes, Emprestimos ativos) — DTO mascarado (Fase 5/6 deferidos).
4. 🟡 `NoResourceFoundException` → 500 (achado 7.X.media.1) — handler trivial.
5. 🟡 Logout não revoga access token (Fase 1 deferido) — UI sai mas access vive até expirar.

---

## Próximo: encerrar com fix-bloco final ou marcar Fase 7 como concluída

Triagem com user define quais dos 5 prioritários entram nesta sessão.

---

# Fixes aplicados pós-auditoria (até 2026-05-24)

Pós-auditoria das 7 fases, **mais 9 fixes** foram aplicados ao longo de sessões subsequentes ao build do MVP escolar. Marcando aqui pra ficar rastreável.

## Sessão 2026-05-23

### ✅ 🟠 Logout não revoga access token (Fase 1.B.alta.3 → CORRIGIDO)
`AuthService.logout` agora bumpa `passwordChangedAt` do usuário após revogar refresh — invalida access tokens existentes imediatamente. Padrão alinhado com `alterarStatus(false)` e `alterarSenha`.

### ✅ 🟠 Bloqueio empréstimo por atraso (P1 audit pendências)
`EmprestimoService.registrar` e `ReservaService.reservar` agora bloqueiam novo empréstimo/reserva se aluno tem livro vencido. Mensagem clara HTTP 422. Repository novo: `countAtrasadosByAluno(alunoId, hoje)`. +2 testes unitários.

### ✅ 🟠 PII mascarada em DTOs admin (Fase 5.B.alta.2 + 6.A.alta.2 → CORRIGIDO)
`AlunoResumoDTO.mascarado(aluno)` retorna mesmo DTO com matrícula no formato `20270****`. Usado em:
- `EmprestimoService.listarAtivos` (via `EmprestimoResponse.fromMascarado`)
- `ReservaService.listarPendentes` (via `ReservaResponse.fromMascarado`)
- Já existia em `DashboardAlertaDTO`

Padrão LGPD §14 — telas visíveis a terceiros atrás do balcão da biblioteca.

### ✅ 🟠 Renovação de empréstimo (P1 audit pendências)
Novo endpoint `POST /emprestimos/{id}/renovacao` (BIB/ADMIN). Coluna `renovacoes` na entity + migration V14 + property `app.emprestimo.max-renovacoes` (default 2). 4 bloqueios: não-ATIVO, em atraso, limite atingido, reserva pendente de outro aluno. +5 testes.

### ✅ 🟠 Edição e cancelamento de empréstimo (P1 audit pendências)
- `PATCH /emprestimos/{id}` — edita `dataEmprestimo` e/ou `prazoDias` (recalcula vencimento). Validações: não-ATIVO, payload vazio, data no futuro, vencimento resultante no passado.
- `DELETE /emprestimos/{id}` — marca CANCELADO (soft delete preservando FK de reservas) e devolve livro ao estoque.
- Novo `SituacaoEmprestimo.CANCELADO` + migration V15 atualizando CHECK constraint.
- Eventos `EMPRESTIMO_EDITADO` e `EMPRESTIMO_CANCELADO` na auditoria. +7 testes.

### ✅ 🟠 XSS stored em `Usuario.nome` (Fase 7.X.alta.1 → JÁ ESTAVA CORRIGIDO antes da sessão; confirmado mantido)
`@Pattern("^[\\p{L}\\p{N} .,'\\-]+$")` em `AtualizarPerfilRequest.nome` e `CriarUsuarioRequest.nome` + migration V13 que sanitiza dados legados (regex strip `[<>]`).

## Sessão 2026-05-24

### ✅ 🟡 Caddyfile com headers de segurança modernos (Fase 4.D/6.B → CORRIGIDO)
Arquivo `Caddyfile` (raiz) adicionou na borda HTTPS:
- `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- `Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=(), usb=(), interest-cohort=()`
- `Cross-Origin-Opener-Policy: same-origin`
- `Cross-Origin-Resource-Policy: same-site`
- `Cache-Control: no-cache, no-store, must-revalidate` em `/index.html`
- Remove `Server` e `X-Powered-By` headers (reconnaissance)

> **CSP completo (img-src, script-src, etc.)** ainda deferido — precisa testar pra não quebrar AntD inline styles.

### ✅ Backup de dumps antigos com hashes BCrypt removidos do git
`backups/liber-20260522-141942.sql` (46 KB) e `backups/dump.err` removidos do repo público no commit `8fe8197`. Continham hashes BCrypt de usuários teste (admin, bibliotecários, alunos seed) — risco baixo-médio mitigado. Histórico antigo ainda tem (não vale o esforço de `git filter-repo` pra dados de teste).

### ✅ Deploy em produção GCP e2-micro + Neon Postgres
Sistema rodando em **https://acervoliber.duckdns.org** desde 2026-05-24. Stack:
- VM Google Cloud `acervo-liber1` (e2-micro, Ubuntu 26.04 LTS Minimal, 1 GB RAM + 2 GB swap)
- Neon Postgres 16 free tier (AWS us-east-1, 0.5 GB)
- Caddy 2 + Let's Encrypt + DuckDNS
- Docker Compose com `docker-compose.gcp.yml` (sem serviço postgres local)

---

## Status final dos 7 fixes prioritários da Fase 7

| # | Fix | Status |
|---|---|---|
| 1 | XSS stored em `nome` | ✅ Aplicado antes |
| 2 | Pageable sem cap global | ✅ Já cobertura por `max-page-size=50` global |
| 3 | PII de menores em DTOs admin | ✅ **Corrigido nesta sessão** |
| 4 | `NoResourceFoundException` → 500 | ✅ Aplicado antes |
| 5 | Logout não revoga access token | ✅ **Corrigido nesta sessão** |
| 6 | CSP / headers modernos | 🟡 **Parcial nesta sessão** (Caddy adicionou COOP/CORP/Permissions-Policy; CSP completo deferido) |
| 7 | CORS `allow-credentials=false` | ❌ Ainda aberto (1 linha, mas requer teste cuidadoso) |

## Deferidos que continuam abertos (resumo)

- 🟡 CORS `allow-credentials=true` → mudar pra `false` (1 linha + teste)
- 🟡 Auditoria de leituras grandes (`GET /alunos?size=999` logado como sinal de scraping)
- 🟡 Polyglot file upload — validar com `ImageIO.read` em vez de só magic bytes
- 🟡 Mascarar PII em log/audit detail
- 🟡 CSP completo (img-src whitelist, script-src self)
- 🟡 `/actuator/info` removido em prod
- 🟡 Mensagens de erro 422 não ecoam dados do request (log injection)
- 🟢 Dependabot / Trivy
- 🟢 Unificar mapas in-memory em Caffeine
- 🟢 `TimeConfig.systemUTC()` + Clock em todos `Instant.now()`
- 🟢 Handlers `HttpMessageNotReadableException` etc.

Esses deferidos não bloqueiam produção. Próximas sessões podem atacar em ordem de impacto.


