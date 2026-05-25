import { Component, type ErrorInfo, type ReactNode } from 'react';
import { Button, Result } from 'antd';

interface Props {
  children: ReactNode;
}

interface State {
  erro: Error | null;
}

/**
 * Boundary de erro do React. Sem isto, qualquer excecao nao tratada num
 * componente vira tela em branco em prod — o React desmonta toda a arvore.
 * Aqui interceptamos, mostramos mensagem amigavel e botao de recarregar.
 *
 * Cobre o caso classico de campo opcional no backend que o frontend assume
 * existir (TypeError "Cannot read property of undefined") apos um deploy
 * com schema novo.
 */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { erro: null };

  static getDerivedStateFromError(erro: Error): State {
    return { erro };
  }

  componentDidCatch(erro: Error, info: ErrorInfo) {
    // Console em prod ja ajuda em debug remoto (user envia print do devtools).
    console.error('ErrorBoundary capturou:', erro, info);
  }

  render() {
    if (this.state.erro) {
      return (
        <Result
          status="error"
          title="Algo deu errado"
          subTitle="Tente recarregar a página. Se o problema continuar, avise o responsável técnico."
          extra={[
            <Button
              key="reload"
              type="primary"
              onClick={() => window.location.reload()}
            >
              Recarregar página
            </Button>,
            <Button
              key="home"
              onClick={() => {
                window.location.href = '/';
              }}
            >
              Ir para o início
            </Button>,
          ]}
        />
      );
    }
    return this.props.children;
  }
}
