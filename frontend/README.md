# AcervoLiber — Frontend

Interface web do sistema AcervoLiber. SPA em **React + TypeScript + Vite**,
componentes do **Ant Design**, consumindo a API REST do backend Spring Boot.

## Pré-requisitos

- Node.js 20+ (testado com Node 22)
- O backend rodando (via `docker compose up` na raiz do projeto, ou local)

## Como rodar

```bash
cd frontend

# 1) Instalar dependências (só na primeira vez)
npm install

# 2) Configurar a URL da API (opcional — o padrão já é http://localhost:8080)
copy .env.example .env

# 3) Subir em modo desenvolvimento
npm run dev
```

A aplicação abre em **http://localhost:5173**.

Login inicial: `admin@liber.local` / `@Admin2026`

## Scripts

| Comando | O que faz |
|---|---|
| `npm run dev` | Servidor de desenvolvimento com hot-reload |
| `npm run build` | Compila TypeScript e gera o build de produção em `dist/` |
| `npm run preview` | Serve o build de produção localmente |

## Estrutura

```
src/
├── main.tsx            Ponto de entrada
├── App.tsx             Providers (React Query, Ant Design, Router)
├── router.tsx          Definição das rotas
├── config.ts           Configuração (URL da API)
├── types/api.ts        Tipos TS espelhando os DTOs do backend
├── api/
│   ├── http.ts         Cliente Axios + interceptor de refresh token
│   ├── auth.ts         Endpoints de autenticação
│   └── dashboard.ts    Endpoint do dashboard
├── auth/
│   ├── authStore.ts    Estado de sessão (Zustand, persistido)
│   └── ProtectedRoute  Guarda de rota — exige login
├── components/
│   └── AppLayout.tsx   Menu lateral + cabeçalho
└── pages/
    ├── LoginPage       Tela de login (funcional)
    ├── DashboardPage   Dashboard com dados reais
    └── Livros/Alunos/Emprestimos/Historico  (em construção)
```

## Como funciona a autenticação

1. `LoginPage` chama `POST /auth/login` → recebe `accessToken` (15min) + `refreshToken` (7 dias).
2. Os tokens ficam no `authStore` (persistido em `localStorage`).
3. Toda request anexa `Authorization: Bearer <accessToken>` (interceptor em `api/http.ts`).
4. Quando o access token expira, o servidor responde `401`. O interceptor
   automaticamente chama `POST /auth/refresh`, atualiza os tokens e repete a
   request original — transparente para as telas.
5. Se o refresh também falhar, a sessão é encerrada e o usuário volta ao login.

## Próximas telas a implementar

As telas Livros, Alunos, Empréstimos e Histórico estão como placeholder.
O padrão para construí-las: criar `api/<recurso>.ts`, usar `useQuery`/`useMutation`
do TanStack Query e a `Table` do Ant Design (que consome `Page<T>` do backend).
