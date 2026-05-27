import { http } from './http';
import type { AlunoResponse, Page } from '../types/api';

/** Dados enviados ao criar/editar um aluno. */
export interface AlunoPayload {
  /** CPF do aluno (com ou sem mascara — backend normaliza). */
  cpf: string;
  nome: string;
  turma: string;
}

export interface ListarAlunosParams {
  termo?: string;
  page?: number;
  size?: number;
}

export async function listarAlunos(params: ListarAlunosParams): Promise<Page<AlunoResponse>> {
  const resp = await http.get<Page<AlunoResponse>>('/alunos', { params });
  return resp.data;
}

export async function criarAluno(payload: AlunoPayload): Promise<AlunoResponse> {
  const resp = await http.post<AlunoResponse>('/alunos', payload);
  return resp.data;
}

export async function atualizarAluno(id: number, payload: AlunoPayload): Promise<AlunoResponse> {
  const resp = await http.put<AlunoResponse>(`/alunos/${id}`, payload);
  return resp.data;
}

export async function removerAluno(id: number): Promise<void> {
  await http.delete(`/alunos/${id}`);
}
