import { http } from './http';
import type { LoginResponse, Usuario } from '../types/api';

export async function login(email: string, senha: string): Promise<LoginResponse> {
  const resp = await http.post<LoginResponse>('/auth/login', { email, senha });
  return resp.data;
}

/** Login do aluno — por matricula. */
export async function loginAluno(matricula: string, senha: string): Promise<LoginResponse> {
  const resp = await http.post<LoginResponse>('/auth/login-aluno', { matricula, senha });
  return resp.data;
}

export async function logout(refreshToken: string): Promise<void> {
  await http.post('/auth/logout', { refreshToken });
}

export async function getUsuarioAtual(): Promise<Usuario> {
  const resp = await http.get<Usuario>('/auth/me');
  return resp.data;
}

export async function trocarSenha(senhaAtual: string, senhaNova: string): Promise<void> {
  await http.post('/auth/change-password', { senhaAtual, senhaNova });
}

export async function atualizarPerfil(nome: string): Promise<Usuario> {
  const resp = await http.put<Usuario>('/auth/perfil', { nome });
  return resp.data;
}
