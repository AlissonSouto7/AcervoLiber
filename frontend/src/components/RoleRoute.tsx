import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../auth/authStore';
import type { Role } from '../types/api';

/**
 * Guarda de rota por perfil. Se o usuario logado nao tiver um dos perfis
 * permitidos, redireciona.
 *
 * - Role conhecido fora dos permitidos -> volta para "/" (que reencaminha
 *   o usuario para a area apropriada do seu perfil).
 * - Role `undefined` -> sessao corrompida; manda direto para /login em vez
 *   de cair em loop com HomeRedirect.
 */
export function RoleRoute({ permitido }: { permitido: Role[] }) {
  const role = useAuthStore((s) => s.usuario?.role);

  if (!role) {
    return <Navigate to="/login" replace />;
  }
  if (!permitido.includes(role)) {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
