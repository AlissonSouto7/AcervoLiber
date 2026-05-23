import { Navigate } from 'react-router-dom';
import { AppLayout } from '../components/AppLayout';
import { useAuthStore } from './authStore';

/**
 * Guarda de rota:
 *  - sem sessao            -> /login
 *  - senha provisoria      -> /primeiro-acesso (troca obrigatoria)
 *  - caso contrario        -> layout da aplicacao
 */
export function ProtectedRoute() {
  const accessToken = useAuthStore((s) => s.accessToken);
  const deveTrocarSenha = useAuthStore((s) => s.usuario?.deveTrocarSenha);

  if (!accessToken) {
    return <Navigate to="/login" replace />;
  }
  if (deveTrocarSenha) {
    return <Navigate to="/primeiro-acesso" replace />;
  }
  return <AppLayout />;
}
