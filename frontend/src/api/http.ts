import axios, { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios';
import { queryClient } from '../App';
import { API_BASE } from '../config';
import { useAuthStore } from '../auth/authStore';
import type { LoginResponse } from '../types/api';

/**
 * Cliente HTTP central. Dois interceptors:
 *  1) Request  — anexa o access token (Authorization: Bearer ...)
 *  2) Response — em 401, tenta renovar via /auth/refresh e repete a request.
 *
 * O fluxo de refresh casa com o backend: access token curto (15min) +
 * refresh token rotacionavel.
 */
export const http = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

// --- Interceptor de request: anexa o token ---
http.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// --- Renovacao de token (uma por vez, mesmo com varias requests em 401) ---
let refreshPromise: Promise<string> | null = null;

function renovarAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const refreshToken = useAuthStore.getState().refreshToken;
      if (!refreshToken) {
        throw new Error('Sem refresh token');
      }
      // axios "cru" (sem os interceptors) para nao recursar
      const resp = await axios.post<LoginResponse>(`${API_BASE}/auth/refresh`, { refreshToken });
      useAuthStore.getState().setTokens({
        accessToken: resp.data.accessToken,
        refreshToken: resp.data.refreshToken,
      });
      return resp.data.accessToken;
    })().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

// --- Interceptor de response: trata 401 com refresh + retry ---
http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;
    const url = original?.url ?? '';
    const ehRotaDeAuth = url.includes('/auth/login') || url.includes('/auth/refresh');

    if (error.response?.status === 401 && original && !original._retry && !ehRotaDeAuth) {
      original._retry = true;
      try {
        const novoToken = await renovarAccessToken();
        // headers pode ser undefined em alguns erros de rede — usar AxiosHeaders
        // de forma defensiva evita TypeError que seria silenciosamente engolido
        // pelo catch abaixo, causando logout indevido.
        if (!original.headers) {
          original.headers = new AxiosHeaders();
        }
        original.headers.set('Authorization', `Bearer ${novoToken}`);
        return http(original);
      } catch {
        // Refresh falhou — encerra a sessao, limpa cache de dados e vai para o login
        useAuthStore.getState().limparSessao();
        queryClient.clear();
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  },
);

/** Extrai uma mensagem de erro amigavel de uma falha do axios. */
export function mensagemDeErro(erro: unknown, padrao = 'Ocorreu um erro inesperado'): string {
  if (axios.isAxiosError(erro)) {
    const detail = (erro.response?.data as { detail?: string } | undefined)?.detail;
    return detail ?? erro.message ?? padrao;
  }
  return padrao;
}
