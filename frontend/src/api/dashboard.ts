import { http } from './http';
import type { DashboardResponse } from '../types/api';

export async function getDashboard(): Promise<DashboardResponse> {
  const resp = await http.get<DashboardResponse>('/dashboard');
  return resp.data;
}
