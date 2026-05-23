import { Tag } from 'antd';
import type { StatusUrgencia } from '../types/api';

const CONFIG: Record<StatusUrgencia, { cor: string; texto: string }> = {
  VERDE: { cor: 'green', texto: 'Em dia' },
  AMARELO: { cor: 'gold', texto: 'Devolucao proxima' },
  VERMELHO: { cor: 'red', texto: 'Atrasado' },
  DEVOLVIDO: { cor: 'default', texto: 'Devolvido' },
};

/** Tag colorida (semaforo) do status de urgencia de um emprestimo. */
export function StatusUrgenciaTag({ status }: { status: StatusUrgencia }) {
  // Fallback defensivo: backend pode adicionar valor novo antes do deploy do
  // frontend casar. Sem o fallback, `CONFIG[status]` undefined quebra o
  // destructuring com TypeError e derruba a tabela inteira.
  const { cor, texto } = CONFIG[status] ?? { cor: 'default', texto: String(status ?? '—') };
  return <Tag color={cor}>{texto}</Tag>;
}
