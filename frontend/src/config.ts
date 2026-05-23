/**
 * Configuracao central da aplicacao.
 * A URL da API vem da variavel de ambiente VITE_API_URL (ver .env.example).
 */
export const API_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export const API_BASE = `${API_URL}/api/v1`;

/**
 * Resolve uma URL de capa para uso em <img src>.
 *
 * Capas externas (Google Books / Open Library) ja vem absolutas (https://...) e
 * passam direto. Capas enviadas manualmente vem como caminho relativo da API
 * (ex.: "/api/v1/livros/5/capa-imagem") e precisam do prefixo da API.
 * URLs blob:/data: (preview local de upload) tambem passam direto.
 */
export function resolverUrlCapa(url: string): string {
  return url.startsWith('/') ? `${API_URL}${url}` : url;
}
