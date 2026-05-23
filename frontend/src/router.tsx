import { lazy, Suspense, type ComponentType } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { Spin } from 'antd';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { RoleRoute } from './components/RoleRoute';
import { HomeRedirect } from './components/HomeRedirect';

// Cada pagina vira um chunk proprio, carregado sob demanda na navegacao.
const LoginPage = lazy(() => import('./pages/LoginPage'));
const PrimeiroAcessoPage = lazy(() => import('./pages/PrimeiroAcessoPage'));
const DashboardPage = lazy(() => import('./pages/DashboardPage'));
const LivrosPage = lazy(() => import('./pages/LivrosPage'));
const AlunosPage = lazy(() => import('./pages/AlunosPage'));
const EmprestimosPage = lazy(() => import('./pages/EmprestimosPage'));
const ReservasPendentesPage = lazy(() => import('./pages/ReservasPendentesPage'));
const HistoricoPage = lazy(() => import('./pages/HistoricoPage'));
const ConfiguracoesPage = lazy(() => import('./pages/ConfiguracoesPage'));
const UsuariosPage = lazy(() => import('./pages/UsuariosPage'));
const AuditoriaPage = lazy(() => import('./pages/AuditoriaPage'));
const CatalogoPage = lazy(() => import('./pages/CatalogoPage'));
const MinhasReservasPage = lazy(() => import('./pages/MinhasReservasPage'));

/** Envolve uma pagina lazy num limite de Suspense com spinner centralizado. */
function comSuspense(Pagina: ComponentType) {
  return (
    <Suspense
      fallback={
        <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
          <Spin size="large" />
        </div>
      }
    >
      <Pagina />
    </Suspense>
  );
}

export const router = createBrowserRouter([
  {
    path: '/login',
    element: comSuspense(LoginPage),
  },
  {
    // Troca de senha obrigatoria — fora do AppLayout (tela cheia).
    path: '/primeiro-acesso',
    element: comSuspense(PrimeiroAcessoPage),
  },
  {
    path: '/',
    element: <ProtectedRoute />,
    children: [
      { index: true, element: <HomeRedirect /> },
      // Disponivel para qualquer perfil autenticado
      { path: 'configuracoes', element: comSuspense(ConfiguracoesPage) },

      // Equipe (ADMIN + BIBLIOTECARIO)
      {
        element: <RoleRoute permitido={['ADMIN', 'BIBLIOTECARIO']} />,
        children: [
          { path: 'dashboard', element: comSuspense(DashboardPage) },
          { path: 'livros', element: comSuspense(LivrosPage) },
          { path: 'alunos', element: comSuspense(AlunosPage) },
          { path: 'emprestimos', element: comSuspense(EmprestimosPage) },
          { path: 'reservas', element: comSuspense(ReservasPendentesPage) },
          { path: 'historico', element: comSuspense(HistoricoPage) },
        ],
      },

      // Apenas ADMIN
      {
        element: <RoleRoute permitido={['ADMIN']} />,
        children: [
          { path: 'usuarios', element: comSuspense(UsuariosPage) },
          { path: 'auditoria', element: comSuspense(AuditoriaPage) },
        ],
      },

      // Apenas ALUNO
      {
        element: <RoleRoute permitido={['ALUNO']} />,
        children: [
          { path: 'catalogo', element: comSuspense(CatalogoPage) },
          { path: 'minhas-reservas', element: comSuspense(MinhasReservasPage) },
        ],
      },
    ],
  },
  {
    path: '*',
    element: <Navigate to="/" replace />,
  },
]);
