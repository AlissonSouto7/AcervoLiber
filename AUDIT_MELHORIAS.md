# Audit de Melhorias — AcervoLiber

> Qualidade, performance, manutenibilidade e UX (somente leitura). Data: 2026-05-22.
> Bugs e segurança ficam fora deste documento (ver `AUDIT_BUGS.md` / `AUDIT_SEGURANCA.md`).

---

## Performance — Backend

- **[Alto]** `DashboardService.java:41-53` — `obter()` carrega **todos** os empréstimos ativos (com join de livro+aluno) só pra filtrar em memória por AMARELO/VERMELHO. Numa biblioteca com centenas de ativos isso pesa a cada acesso. Usar duas queries que já filtram por faixa de data, com `LIMIT`. Esforço: médio.
- **[Médio]** `EmprestimoRepository.java:45-53` — `rankingLivrosMaisEmprestados` faz `GROUP BY` sobre toda a tabela `emprestimos` sem recorte temporal. Aceitar período (ex.: últimos 12 meses) como parâmetro. Esforço: pequeno.
- **[Médio]** `AlunoService.java:37-40` — `listar()` chama `contarEmprestimosAtivos(id)` por aluno → N+1 (11 queries para 10 alunos). Usar query agregada `(alunoId, count)` ou projeção com `LEFT JOIN ... GROUP BY`. Esforço: médio.
- **[Baixo]** `V7__create_reservas.sql` — falta índice em `data_expiracao`. O job `expirarVencidas` filtra `status = PENDENTE AND dataExpiracao < hoje`. Índice composto `(status, data_expiracao)`. Esforço: pequeno (nova migration).

## Qualidade — Backend

- **[Médio]** `LivroService.java` (217 linhas) — mistura CRUD de livro, regras de estoque e toda a gestão de capa (upload, validação, leitura, remoção, backfill). Extrair `CapaLivroService`. Esforço: médio.
- **[Médio]** `StatusUrgencia.java:16-28` — regra de negócio (faixas de urgência) morando no pacote `dto`, com limiar `DIAS_AMARELO = 2` fixo no enum enquanto prazos/limites estão em `EmprestimoProperties`. Mover o cálculo para domínio/serviço e tornar o limiar configurável. Esforço: pequeno.
- **[Médio]** Mapeamento entity→DTO manual repetido em todos os `*Response.from(...)`. Funciona e é consistente — se o nº de DTOs crescer, considerar MapStruct. Esforço: médio.
- **[Baixo]** `LivroService.java:103-105` — `java.util.Objects.equals(...)` totalmente qualificado inline, inconsistente com o resto. Adicionar `import`.
- **[Baixo]** `LivroService.java:180` — URL da capa montada à mão duplica o path do `LivroController`; se a rota mudar, quebra silenciosamente. Extrair constante / `UriComponentsBuilder`.
- **[Baixo]** `LivroService.java:188` — `@Transactional(readOnly=true)` redundante (a classe já declara no topo).
- **[Baixo]** `V1__init.sql:2` — comentário diz "schema inicial do BiblioEscola" (nome de outro projeto).

## Performance — Frontend

- **[Alto]** `App.tsx:13-20` — `QueryClient` sem `staleTime`/`gcTime`. Com `staleTime` default 0, navegar entre páginas refaz toda request imediatamente. Definir `staleTime: 30_000` global. **Uma linha, alto retorno.** Esforço: pequeno.
- **[Médio]** `EmprestimosPage.tsx:48-56,83-92` — o drawer de novo empréstimo carrega `listarLivros/Alunos({ size: 100 })`; acima de 100, opções somem da busca. Usar busca server-side no `Select` (debounce + `onSearch`). Esforço: médio.
- **[Médio]** `LivrosPage.tsx`/`EmprestimosPage.tsx` — arrays de `colunas` e funções de render recriados a cada render. Impacto real baixo (listas pequenas), mas mover para fora do componente / `useMemo` é boa prática. Esforço: pequeno.
- **[Baixo]** `CapaLivro.tsx:85-100` — `<img loading="lazy">` (bom) mas sem `decoding="async"`. Esforço: pequeno.

## Qualidade — Frontend

- **[Alto]** Padrão de página CRUD duplicado em 5+ telas (`Livros`, `Alunos`, `Usuarios`, `Emprestimos`, `Auditoria`): estado `termo`/`page`, objeto `paginacao`, cabeçalho flex com style inline idêntico, alternância `isMobile ? <List> : <Table>`, drawer com form. Extrair hook `usePaginacao()`, componente `<CabecalhoPagina>` e `<TabelaResponsiva>`. Reduz centenas de linhas. Esforço: grande (alto retorno).
- **[Médio]** Tratamento de loading/erro inconsistente — `DashboardPage` trata `isError` com `<Alert>`; as páginas de tabela (`Livros`, `Alunos`, `Historico`) **não tratam `isError`** — uma falha de rede vira "Nenhum livro encontrado". Criar `<EstadoConsulta>` e sempre tratar `isError`. Esforço: médio.
- **[Médio]** Invalidação de query keys duplicada e frágil — cada tela mantém sua lista de keys (`CHAVES_RELACIONADAS`, `invalidarTudo`) com strings soltas; fácil esquecer uma. Centralizar num `queryKeys.ts` com helpers por domínio. Esforço: médio.
- **[Médio]** Mapas de status (cor/texto) repetidos — `StatusReserva`, `EventoAuditoria`, `SituacaoEmprestimo`, `StatusUrgencia`, cada um em página diferente. Agrupar em `statusMaps.ts` + componente `<TagStatus>`. Esforço: pequeno.
- **[Baixo]** `ConfiguracoesPage.tsx:13-40` — usa `useState` + `try/finally` manual para loading, diferente de todas as outras telas que usam `useMutation`. Padronizar. Esforço: pequeno.
- **[Baixo]** `types/api.ts:117` — `DashboardResponse.alertasProximaDevolucao` declarado mas a UI ignora os "próximos". Ou renderizar (info útil) ou remover do payload. Esforço: pequeno.

## UX

- **[Médio]** Falha de rede vira "estado vazio" enganoso nas páginas de tabela — o usuário pensa que não há dados quando a chamada falhou. Exibir `<Alert type="error">` com botão "Tentar novamente" (`refetch`). Esforço: médio.
- **[Baixo]** `LivrosPage.tsx:218-228` — botão de remover não mostra `loading` durante a mutation (`remover.isPending`), diferente do botão de confirmar reserva. Padronizar `loading` nas ações destrutivas. Esforço: pequeno.
- **[Baixo]** `LivrosPage.tsx:217,225-227` — `<EditOutlined onClick>`/`<DeleteOutlined>` clicáveis sem `aria-label`; leitores de tela anunciam só "ícone". Envolver em `<Button>` com `aria-label`. Esforço: pequeno.
- **[Baixo]** `http.ts:79-85` — erro de rede (sem `response`) cai em "Network Error" do axios, pouco amigável. Detectar ausência de `response` e devolver mensagem PT-BR. Esforço: pequeno.
- **[Baixo]** `ReservasPendentesPage.tsx:24` — `PRAZO_PADRAO = 14` hardcoded no frontend, enquanto o backend usa `prazo-padrao-dias=7`. Alinhar ou expor via endpoint de config. Esforço: pequeno.

## Arquitetura

- **[Médio]** Pacotes por camada técnica (`controller/service/dto/entity/repository`) — espalha cada feature em 5 pacotes. Gerenciável neste tamanho; se o portal do aluno crescer, considerar pacotes por feature. Esforço: grande.
- **[Baixo]** `ReservaService.java:47-55` — 9 dependências injetadas (5 repos + 2 properties + `EmprestimoService` + `Clock`); faz ações do aluno + do bibliotecário + job de expiração. Separar responsabilidades. Esforço: médio.
- **[Baixo]** Cálculo "limite − ativos − pendentes" duplicado front/back. O backend poderia entregar `vagasDisponiveis` pronto no `ReservaResumoResponse`. Esforço: pequeno.

## DX / Manutenibilidade

- **[Médio]** Sem ESLint/Prettier no frontend — só scripts `dev`/`build`/`preview`. O estilo está consistente por disciplina manual; sem lint, degrada com o tempo e não há checagem no CI. Adicionar `eslint` + `@typescript-eslint` + `prettier` + script `lint`. Esforço: médio.
- **[Baixo]** `package.json` — `"build": "tsc && vite build"`; adicionar script `type-check` separado (`tsc --noEmit`) para CI/pre-commit. Esforço: pequeno.

---

## Prioridades — alto impacto, baixo/médio esforço

1. `staleTime` global no TanStack Query (`App.tsx`) — uma linha, corta refetches.
2. Tratar `isError` nas páginas de tabela — evita "estado vazio" enganoso.
3. Dashboard: queries filtradas por data em vez de carregar todos os ativos.
4. N+1 na listagem de alunos — query agregada de contagem.
5. Extrair `<CabecalhoPagina>`, `usePaginacao`, `queryKeys.ts` — paga em todas as telas.

## Pontos fortes do código

`@EntityGraph` evitando N+1 em empréstimos/reservas · decremento atômico de estoque via JPQL · `open-in-view=false` · `max-page-size` contra DoS · `GlobalExceptionHandler` com ProblemDetail (RFC 7807) · `CapaService` com cache e fallback bem documentados · code-splitting por rota no frontend. A base é sólida; as melhorias acima são incrementais.
