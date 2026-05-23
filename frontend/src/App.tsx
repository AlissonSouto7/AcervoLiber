import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App as AntdApp, ConfigProvider } from 'antd';
import ptBR from 'antd/locale/pt_BR';
import { RouterProvider } from 'react-router-dom';
import { router } from './router';

/**
 * Componente raiz — monta os providers:
 *  - QueryClientProvider: cache de chamadas a API (TanStack Query)
 *  - ConfigProvider: tema e locale (pt-BR) do Ant Design
 *  - AntdApp: habilita message/notification com o tema correto
 */
/**
 * QueryClient exportado para que o logout (e qualquer outro fluxo que mude
 * a identidade do usuario) possa chamar `queryClient.clear()` — senao, o cache
 * em memoria sobrevive a troca de usuario, vazando PII entre sessoes no mesmo
 * computador (cenario tipico de PC compartilhado em escola).
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={ptBR}>
        <AntdApp>
          <RouterProvider router={router} />
        </AntdApp>
      </ConfigProvider>
    </QueryClientProvider>
  );
}
