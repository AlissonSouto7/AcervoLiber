import { http } from './http';
import type { AuditLogResponse, EventoAuditoria, Page } from '../types/api';

export interface ListarAuditoriaParams {
  evento?: EventoAuditoria;
  page?: number;
  size?: number;
}

export async function listarAuditoria(params: ListarAuditoriaParams): Promise<Page<AuditLogResponse>> {
  const resp = await http.get<Page<AuditLogResponse>>('/auditoria', { params });
  return resp.data;
}
