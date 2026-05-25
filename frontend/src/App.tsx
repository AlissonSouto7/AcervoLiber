import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App as AntdApp, ConfigProvider } from 'antd';
import ptBR from 'antd/locale/pt_BR';
import { RouterProvider } from 'react-router-dom';
import { ErrorBoundary } from './components/ErrorBoundary';
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
      // staleTime de 30s: evita refetch agressivo ao trocar de tela (eg.: voltar
      // pra Dashboard depois de criar emprestimo). Sem isto, qualquer remount
      // dispara nova requisicao mesmo com dado fresco no cache.
      staleTime: 30_000,
    },
  },
});

export default function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <ConfigProvider locale={ptBR}>
          <AntdApp>
            <RouterProvider router={router} />
          </AntdApp>
        </ConfigProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}
