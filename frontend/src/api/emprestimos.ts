import { http } from './http';
import type { EmprestimoResponse, Page } from '../types/api';

/** Dados enviados ao registrar um emprestimo. */
export interface EmprestimoPayload {
  /** ID do exemplar fisico (copia especifica) — escolhido pelo bibliotecario. */
  exemplarId: number;
  alunoId: number;
  prazoDias: number;
}

export async function listarEmprestimosAtivos(): Promise<EmprestimoResponse[]> {
  const resp = await http.get<EmprestimoResponse[]>('/emprestimos/ativos');
  return resp.data;
}

export async function listarHistorico(params: { page?: number; size?: number }): Promise<Page<EmprestimoResponse>> {
  const resp = await http.get<Page<EmprestimoResponse>>('/emprestimos/historico', { params });
  return resp.data;
}

export async function registrarEmprestimo(payload: EmprestimoPayload): Promise<EmprestimoResponse> {
  const resp = await http.post<EmprestimoResponse>('/emprestimos', payload);
  return resp.data;
}

export async function devolverEmprestimo(id: number): Promise<EmprestimoResponse> {
  const resp = await http.post<EmprestimoResponse>(`/emprestimos/${id}/devolucao`);
  return resp.data;
}
