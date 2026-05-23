/**
 * Tipos TypeScript que espelham os DTOs do backend (Spring Boot).
 * Mantenha em sincronia com com.liber.dto.*
 */

export type Role = 'ADMIN' | 'BIBLIOTECARIO' | 'ALUNO';

export type SituacaoEmprestimo = 'ATIVO' | 'DEVOLVIDO';

export type StatusUrgencia = 'VERDE' | 'AMARELO' | 'VERMELHO' | 'DEVOLVIDO';

export type StatusReserva = 'PENDENTE' | 'CONFIRMADA' | 'RECUSADA' | 'CANCELADA' | 'EXPIRADA';

export type EventoAuditoria =
  | 'LOGIN_SUCESSO'     // legado — nao registrado mais, mantido para registros antigos
  | 'LOGIN_FALHA'
  | 'LOGIN_BLOQUEADO'
  | 'LOGOUT'
  | 'TROCA_SENHA'
  | 'PERFIL_ATUALIZADO'
  | 'USUARIO_CRIADO'
  | 'USUARIO_ATIVADO'
  | 'USUARIO_DESATIVADO'
  | 'REFRESH_REUSO'
  | 'ACESSO_NEGADO'
  | 'EMPRESTIMO_REGISTRADO'
  | 'EMPRESTIMO_DEVOLVIDO'
  | 'ESTOQUE_DIVERGENCIA';

export interface AuditLogResponse {
  id: number;
  evento: EventoAuditoria;
  /** Alvo do evento (login tentado, usuario criado/desativado, etc.). */
  usuarioEmail: string | null;
  /** Quem EXECUTOU a acao (capturado do SecurityContext). Nullable em eventos sem auth. */
  atorEmail: string | null;
  ip: string | null;
  userAgent: string | null;
  detalhe: string | null;
  ocorridoEm: string;
}

export interface Usuario {
  id: number;
  email: string;
  nome: string;
  role: Role;
  ativo: boolean;
  deveTrocarSenha: boolean;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
  usuario: Usuario;
}

export interface LivroResponse {
  id: number;
  titulo: string;
  autor: string;
  isbn: string | null;
  ano: number | null;
  quantidadeExemplares: number;
  quantidadeDisponivel: number;
  /** URL da capa (externa automatica ou imagem enviada); null se nao houver. */
  capaUrl: string | null;
  /** true quando a capa foi enviada manualmente (nao e sobrescrita pela busca). */
  capaManual: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AlunoResponse {
  id: number;
  matricula: string;
  nome: string;
  turma: string;
  livrosEmprestadosAtualmente: number;
  createdAt: string;
  updatedAt: string;
}

export interface LivroResumo {
  id: number;
  titulo: string;
  autor: string;
}

export interface AlunoResumo {
  id: number;
  matricula: string;
  nome: string;
  turma: string;
}

export interface EmprestimoResponse {
  id: number;
  livro: LivroResumo;
  aluno: AlunoResumo;
  dataEmprestimo: string;
  prazoDias: number;
  dataDevolucaoPrevista: string;
  dataDevolucaoEfetiva: string | null;
  situacao: SituacaoEmprestimo;
  statusUrgencia: StatusUrgencia;
}

export interface LivroRanking {
  livroId: number;
  titulo: string;
  autor: string;
  totalEmprestimos: number;
}

export interface DashboardAlertaDTO {
  emprestimoId: number;
  livroTitulo: string;
  alunoNome: string;
  /** Matricula com os ultimos digitos mascarados (Fase 7 fix LGPD). */
  alunoMatriculaMascarada: string;
  alunoTurma: string | null;
  dataEmprestimo: string;
  dataDevolucaoPrevista: string;
  diasAtraso: number;
  statusUrgencia: 'VERDE' | 'AMARELO' | 'VERMELHO';
}

export interface DashboardResponse {
  totais: {
    totalLivros: number;
    totalAlunos: number;
    emprestimosAtivos: number;
    emprestimosAtrasados: number;
  };
  alertasProximaDevolucao: DashboardAlertaDTO[];
  alertasAtrasados: DashboardAlertaDTO[];
  livrosMaisEmprestados: LivroRanking[];
}

export interface ReservaResponse {
  id: number;
  livro: LivroResumo;
  aluno: AlunoResumo;
  status: StatusReserva;
  dataReserva: string;
  dataExpiracao: string;
}

export interface ReservaResumoResponse {
  emprestimosAtivos: number;
  reservasPendentes: number;
  /** Teto combinado de emprestimos + reservas por aluno. */
  limite: number;
}

/** Estrutura de pagina do Spring Data (Page<T>). */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/** Corpo de erro padronizado do backend (RFC 7807). */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
  erros?: { campo: string; mensagem: string }[];
}
