import { http } from './http';
import type { Page, Role, Usuario } from '../types/api';

/** Dados enviados ao criar um usuario (admin). */
export interface CriarUsuarioPayload {
  email: string;
  nome: string;
  senha: string;
  role: Role;
}

export async function listarUsuarios(params: { page?: number; size?: number }): Promise<Page<Usuario>> {
  const resp = await http.get<Page<Usuario>>('/usuarios', { params });
  return resp.data;
}

export async function criarUsuario(payload: CriarUsuarioPayload): Promise<Usuario> {
  const resp = await http.post<Usuario>('/usuarios', payload);
  return resp.data;
}

export async function alterarStatusUsuario(id: number, ativo: boolean): Promise<Usuario> {
  const resp = await http.patch<Usuario>(`/usuarios/${id}/status`, { ativo });
  return resp.data;
}
