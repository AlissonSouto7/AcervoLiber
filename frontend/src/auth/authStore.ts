import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { LoginResponse, Usuario } from '../types/api';

/**
 * Estado de autenticacao, persistido em localStorage para sobreviver a um
 * refresh da pagina.
 *
 * NOTA DE SEGURANCA: localStorage e vulneravel a XSS. Para endurecer, mover o
 * refresh token para um cookie httpOnly (exige ajuste nos endpoints /auth do
 * backend). Aceitavel nesta fase; documentado para evolucao futura.
 */
interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  usuario: Usuario | null;

  setSessao: (resp: LoginResponse) => void;
  setTokens: (tokens: { accessToken: string; refreshToken: string }) => void;
  setUsuario: (usuario: Usuario) => void;
  limparSessao: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      usuario: null,

      setSessao: (resp) =>
        set({
          accessToken: resp.accessToken,
          refreshToken: resp.refreshToken,
          usuario: resp.usuario,
        }),

      setTokens: ({ accessToken, refreshToken }) => set({ accessToken, refreshToken }),

      setUsuario: (usuario) => set({ usuario }),

      limparSessao: () => {
        set({ accessToken: null, refreshToken: null, usuario: null });
        // Defesa: se o persist falhar (cota cheia, modo privado), o set() acima
        // pode nao chegar ao localStorage. Garante a remocao via API direta.
        try {
          localStorage.removeItem('liber-auth');
        } catch {
          /* localStorage indisponivel — nada a fazer */
        }
      },
    }),
    { name: 'liber-auth' },
  ),
);
