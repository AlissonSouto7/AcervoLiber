/**
 * Converte data ISO local (yyyy-MM-dd) para dd/MM/yyyy, sem deslocamento de fuso.
 * Tolerante a strings ISO completas (yyyy-MM-ddTHH:mm...) — pega so a parte da data.
 * Retorna '—' para null/undefined/string vazia/formato invalido (evita exibir
 * "undefined/undefined/undefined" caso o backend mude o formato).
 */
export function formatarData(iso: string | null | undefined): string {
  if (!iso) {
    return '—';
  }
  // Aceita "2026-05-23" ou "2026-05-23T10:00:00Z" — pega só os 10 primeiros chars.
  const apenasData = iso.length >= 10 ? iso.substring(0, 10) : iso;
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(apenasData);
  if (!match) {
    return '—';
  }
  const [, ano, mes, dia] = match;
  return `${dia}/${mes}/${ano}`;
}

/** Formata um instante ISO completo (data + hora) no fuso local. */
export function formatarDataHora(iso: string | null | undefined): string {
  if (!iso) {
    return '—';
  }
  const data = new Date(iso);
  if (isNaN(data.getTime())) {
    return '—';
  }
  return data.toLocaleString('pt-BR');
}
