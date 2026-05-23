import { http } from './http';

/**
 * Busca de capa de livro.
 *
 * A consulta vai para o NOSSO backend (`GET /livros/capa`), que consulta a
 * Google Books / Open Library com cache. O navegador nunca chama essas APIs
 * diretamente — era isso que estourava o rate limit (HTTP 429).
 *
 * Usado pelo preview do formulario de livro. Na listagem, a capa ja vem pronta
 * no campo `capaUrl` de cada livro.
 */

interface CapaResponse {
  capaUrl: string | null;
}

export interface BuscarCapaParams {
  /** ISBN (opcional) — quando ausente, busca so por titulo+autor. */
  isbn?: string;
  titulo?: string;
  autor?: string;
}

/** Retorna a URL da capa, ou `null` se nenhuma fonte tiver capa. */
export async function buscarCapa({ isbn, titulo, autor }: BuscarCapaParams): Promise<string | null> {
  const isbnLimpo = (isbn ?? '').replace(/[^0-9Xx]/g, '');
  if (!isbnLimpo && !titulo?.trim()) return null;

  const resp = await http.get<CapaResponse>('/livros/capa', {
    params: { isbn: isbnLimpo || undefined, titulo, autor },
  });
  return resp.data.capaUrl;
}
