import { useState } from 'react';
import { App, Alert, Button, Card, Form, Input, Typography } from 'antd';
import { Navigate, useNavigate } from 'react-router-dom';
import { getUsuarioAtual, trocarSenha } from '../api/auth';
import { mensagemDeErro } from '../api/http';
import { useAuthStore } from '../auth/authStore';

interface FormValues {
  senhaAtual: string;
  senhaNova: string;
  confirmar: string;
}

/**
 * Troca de senha obrigatoria no primeiro acesso (senha provisoria).
 * Pagina de tela cheia — o usuario nao navega ate definir a nova senha.
 */
export default function PrimeiroAcessoPage() {
  const navigate = useNavigate();
  const { message } = App.useApp();
  const accessToken = useAuthStore((s) => s.accessToken);
  const usuario = useAuthStore((s) => s.usuario);
  const setUsuario = useAuthStore((s) => s.setUsuario);
  const [carregando, setCarregando] = useState(false);

  async function aoEnviar(values: FormValues) {
    setCarregando(true);
    try {
      await trocarSenha(values.senhaAtual, values.senhaNova);
      const atualizado = await getUsuarioAtual();
      setUsuario(atualizado);
      message.success('Senha definida! Bem-vindo(a).');
      navigate('/', { replace: true });
    } catch (erro) {
      message.error(mensagemDeErro(erro));
    } finally {
      setCarregando(false);
    }
  }

  if (!accessToken) {
    return <Navigate to="/login" replace />;
  }
  if (usuario && !usuario.deveTrocarSenha) {
    return <Navigate to="/" replace />;
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#f0f2f5',
        padding: 16,
      }}
    >
      <Card style={{ width: '100%', maxWidth: 420 }}>
        <Typography.Title level={4} style={{ textAlign: 'center' }}>
          Defina sua senha
        </Typography.Title>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="Por segurança, troque a senha provisória antes de continuar."
        />
        <Form<FormValues> layout="vertical" size="large" onFinish={aoEnviar} disabled={carregando}>
          <Form.Item
            name="senhaAtual"
            label="Senha provisória"
            rules={[{ required: true, message: 'Informe a senha provisória' }]}
          >
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Form.Item
            name="senhaNova"
            label="Nova senha"
            rules={[
              { required: true, message: 'Informe a nova senha' },
              { min: 10, message: 'A senha deve ter ao menos 10 caracteres' },
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Form.Item
            name="confirmar"
            label="Confirmar nova senha"
            dependencies={['senhaNova']}
            rules={[
              { required: true, message: 'Confirme a nova senha' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  return !value || value === getFieldValue('senhaNova')
                    ? Promise.resolve()
                    : Promise.reject(new Error('As senhas não conferem'));
                },
              }),
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={carregando} size="large">
            Salvar e continuar
          </Button>
        </Form>
      </Card>
    </div>
  );
}
