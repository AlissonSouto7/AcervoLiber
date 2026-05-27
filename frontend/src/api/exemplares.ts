import { http } from './http';
import type { ExemplarResponse } from '../types/api';

/** Gestao de copias fisicas de um livro (tombamento). */

export async function listarExemplares(livroId: number): Promise<ExemplarResponse[]> {
  const resp = await http.get<ExemplarResponse[]>(`/livros/${livroId}/exemplares`);
  return resp.data;
}

export async function adicionarExemplar(
  livroId: number,
  codigo?: string,
): Promise<ExemplarResponse> {
  const body = codigo && codigo.trim() ? { codigo: codigo.trim() } : {};
  const resp = await http.post<ExemplarResponse>(`/livros/${livroId}/exemplares`, body);
  return resp.data;
}

export async function renomearExemplar(
  exemplarId: number,
  codigo: string,
): Promise<ExemplarResponse> {
  const resp = await http.put<ExemplarResponse>(`/exemplares/${exemplarId}`, { codigo });
  return resp.data;
}

export async function marcarExtraviado(exemplarId: number): Promise<ExemplarResponse> {
  const resp = await http.post<ExemplarResponse>(`/exemplares/${exemplarId}/extraviar`);
  return resp.data;
}

export async function reativarExemplar(exemplarId: number): Promise<ExemplarResponse> {
  const resp = await http.post<ExemplarResponse>(`/exemplares/${exemplarId}/reativar`);
  return resp.data;
}

export async function removerExemplar(exemplarId: number): Promise<void> {
  await http.delete(`/exemplares/${exemplarId}`);
}
