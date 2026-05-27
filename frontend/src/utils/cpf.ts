/**
 * Aplica mascara CPF (999.999.999-99) ao valor digitado. Usado como `normalize`
 * em Form.Item — o AntD chama esta funcao a cada keystroke e re-escreve o input.
 *
 * Aceita parcial: digitar "123" devolve "123", "1234" devolve "123.4", etc.
 * Limita a 11 digitos (descarta o que passar).
 */
export function mascararCpf(valor: string): string {
  if (!valor) return '';
  const digitos = valor.replace(/\D/g, '').slice(0, 11);
  if (digitos.length <= 3) return digitos;
  if (digitos.length <= 6) return `${digitos.slice(0, 3)}.${digitos.slice(3)}`;
  if (digitos.length <= 9) return `${digitos.slice(0, 3)}.${digitos.slice(3, 6)}.${digitos.slice(6)}`;
  return `${digitos.slice(0, 3)}.${digitos.slice(3, 6)}.${digitos.slice(6, 9)}-${digitos.slice(9)}`;
}

/** So digitos. Util pra checar tamanho real sem a mascara. */
export function digitosCpf(valor: string): string {
  return (valor ?? '').replace(/\D/g, '');
}
