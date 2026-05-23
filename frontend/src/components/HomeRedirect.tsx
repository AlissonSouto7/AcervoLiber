import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../auth/authStore';

/**
 * Pagina inicial — reencaminha conforme o perfil:
 * aluno vai para o catalogo, equipe vai para o dashboard.
 *
 * Defesa: se o `role` chegar `undefined` (corrupcao do localStorage, schema
 * antigo de persist, backend retornando usuario sem role), nao tem para onde
 * mandar — limpa a sessao e leva para /login em vez de deixar a app entrar
 * em loop com RoleRoute (que tambem rejeita role undefined e manda de volta).
 */
export function HomeRedirect() {
  const role = useAuthStore((s) => s.usuario?.role);
  const limparSessao = useAuthStore((s) => s.limparSessao);

  if (!role) {
    limparSessao();
    return <Navigate to="/login" replace />;
  }
  return <Navigate to={role === 'ALUNO' ? '/catalogo' : '/dashboard'} replace />;
}
