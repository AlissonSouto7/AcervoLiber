import { http } from './http';
import type { LivroResponse, Page } from '../types/api';

/** Dados enviados ao criar/editar um livro. */
export interface LivroPayload {
  titulo: string;
  autor: string;
  isbn?: string | null;
  ano?: number | null;
  quantidadeExemplares: number;
  sinopse?: string | null;
}

export interface ListarLivrosParams {
  termo?: string;
  page?: number;
  size?: number;
}

export async function listarLivros(params: ListarLivrosParams): Promise<Page<LivroResponse>> {
  const resp = await http.get<Page<LivroResponse>>('/livros', { params });
  return resp.data;
}

export async function criarLivro(payload: LivroPayload): Promise<LivroResponse> {
  const resp = await http.post<LivroResponse>('/livros', payload);
  return resp.data;
}

export async function atualizarLivro(id: number, payload: LivroPayload): Promise<LivroResponse> {
  const resp = await http.put<LivroResponse>(`/livros/${id}`, payload);
  return resp.data;
}

export async function removerLivro(id: number): Promise<void> {
  await http.delete(`/livros/${id}`);
}

/**
 * Envia uma imagem de capa propria para o livro. axios remove o Content-Type
 * JSON padrao quando o corpo e FormData, deixando o navegador definir o
 * multipart/form-data com boundary.
 */
export async function enviarCapa(id: number, arquivo: File): Promise<LivroResponse> {
  const form = new FormData();
  form.append('file', arquivo);
  const resp = await http.post<LivroResponse>(`/livros/${id}/capa`, form);
  return resp.data;
}

/** Remove a capa enviada manualmente e volta para a capa automatica. */
export async function removerCapaManual(id: number): Promise<LivroResponse> {
  const resp = await http.delete<LivroResponse>(`/livros/${id}/capa`);
  return resp.data;
}
