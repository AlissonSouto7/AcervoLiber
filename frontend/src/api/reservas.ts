import { http } from './http';
import type { Page, ReservaResponse, ReservaResumoResponse } from '../types/api';

// ---------- Aluno ----------

export async function reservarLivro(livroId: number): Promise<ReservaResponse> {
  const resp = await http.post<ReservaResponse>('/reservas', { livroId });
  return resp.data;
}

export async function resumoReservas(): Promise<ReservaResumoResponse> {
  const resp = await http.get<ReservaResumoResponse>('/reservas/resumo');
  return resp.data;
}

export async function listarMinhasReservas(params: { page?: number; size?: number }): Promise<Page<ReservaResponse>> {
  const resp = await http.get<Page<ReservaResponse>>('/reservas/minhas', { params });
  return resp.data;
}

export async function cancelarReserva(id: number): Promise<void> {
  await http.post(`/reservas/${id}/cancelar`);
}

// ---------- Bibliotecario ----------

export async function listarReservasPendentes(params: { page?: number; size?: number } = {}): Promise<Page<ReservaResponse>> {
  const resp = await http.get<Page<ReservaResponse>>('/reservas/pendentes', { params });
  return resp.data;
}

export async function confirmarReserva(
  id: number,
  prazoDias: number,
  exemplarId?: number,
): Promise<ReservaResponse> {
  const resp = await http.post<ReservaResponse>(`/reservas/${id}/confirmar`, {
    prazoDias,
    exemplarId,
  });
  return resp.data;
}

export async function recusarReserva(id: number): Promise<ReservaResponse> {
  const resp = await http.post<ReservaResponse>(`/reservas/${id}/recusar`);
  return resp.data;
}
