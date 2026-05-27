/**
 * Tipos TypeScript que espelham os DTOs do backend (Spring Boot).
 * Mantenha em sincronia com com.liber.dto.*
 */

export type Role = 'ADMIN' | 'BIBLIOTECARIO' | 'ALUNO';

export type SituacaoEmprestimo = 'ATIVO' | 'DEVOLVIDO';

export type StatusUrgencia = 'VERDE' | 'AMARELO' | 'VERMELHO' | 'DEVOLVIDO';

export type StatusReserva = 'PENDENTE' | 'CONFIRMADA' | 'RECUSADA' | 'CANCELADA' | 'EXPIRADA';

export type SituacaoExemplar = 'DISPONIVEL' | 'EMPRESTADO' | 'RESERVADO' | 'EXTRAVIADO';

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
  /** Total de exemplares fisicos cadastrados (em qualquer situacao). */
  exemplaresTotal: number;
  /** Quantos exemplares estao DISPONIVEL pra emprestimo/reserva imediato. */
  exemplaresDisponiveis: number;
  /** URL da capa (externa automatica ou imagem enviada); null se nao houver. */
  capaUrl: string | null;
  /** true quando a capa foi enviada manualmente (nao e sobrescrita pela busca). */
  capaManual: boolean;
  /** Sinopse do livro — editavel pelo bibliotecario ou auto-populada pelo Google Books. */
  sinopse: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ExemplarResponse {
  id: number;
  codigo: string;
  situacao: SituacaoExemplar;
  createdAt: string;
  updatedAt: string;
}

export interface AlunoResponse {
  id: number;
  /** CPF formatado (123.456.789-01) ou mascarado (123.***.***-01) — depende do contexto. */
  cpf: string;
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
  /** ID do exemplar fisico envolvido. Null em contextos puramente bibliograficos (reserva por titulo). */
  exemplarId: number | null;
  /** Codigo de tombamento do exemplar (ex.: LIB-00042). Null se nao aplicavel. */
  exemplarCodigo: string | null;
}

export interface AlunoResumo {
  id: number;
  cpf: string;
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
  /** Codigo de tombamento do exemplar emprestado. */
  exemplarCodigo: string;
  alunoNome: string;
  /** CPF com pontos mascarados (123.***.***-01). */
  alunoCpfMascarado: string;
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
