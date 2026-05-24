import { useState } from 'react';
import { App, Button, Card, Form, Input, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import { queryClient } from '../App';
import { atualizarPerfil, trocarSenha } from '../api/auth';
import { mensagemDeErro } from '../api/http';
import { useAuthStore } from '../auth/authStore';

export default function ConfiguracoesPage() {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const usuario = useAuthStore((s) => s.usuario);
  const setUsuario = useAuthStore((s) => s.setUsuario);
  const limparSessao = useAuthStore((s) => s.limparSessao);

  const [formSenha] = Form.useForm();
  const [salvandoPerfil, setSalvandoPerfil] = useState(false);
  const [salvandoSenha, setSalvandoSenha] = useState(false);

  // Aluno nao pode editar o nome (e o nome oficial cadastrado pela escola).
  // So o bibliotecario pode mudar via tela de Alunos.
  const ehAluno = usuario?.role === 'ALUNO';

  async function salvarPerfil(values: { nome: string }) {
    setSalvandoPerfil(true);
    try {
      const atualizado = await atualizarPerfil(values.nome);
      setUsuario(atualizado);
      message.success('Dados atualizados');
    } catch (erro) {
      message.error(mensagemDeErro(erro));
    } finally {
      setSalvandoPerfil(false);
    }
  }

  async function salvarSenha(values: { senhaAtual: string; senhaNova: string }) {
    setSalvandoSenha(true);
    try {
      await trocarSenha(values.senhaAtual, values.senhaNova);
      // O backend revoga TODOS os refresh tokens ao trocar senha (defesa contra
      // sessao roubada). Continuar usando os tokens locais cairia em cascata de
      // 401 → /refresh → 401 → reuso detectado (poluindo a trilha e dando ao
      // usuario um logout aparentemente aleatorio). Limpa a sessao local e
      // redireciona para login imediatamente.
      message.success('Senha alterada. Entre novamente com a nova senha.');
      limparSessao();
      queryClient.clear();
      navigate('/login', { replace: true });
    } catch (erro) {
      message.error(mensagemDeErro(erro));
      setSalvandoSenha(false);
    }
  }

  return (
    <>
      <Typography.Title level={3}>Configurações</Typography.Title>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 16 }}>
        <Card title="Dados da conta" style={{ flex: '1 1 360px', maxWidth: 480 }}>
          <Form
            layout="vertical"
            initialValues={{ nome: usuario?.nome }}
            onFinish={salvarPerfil}
            disabled={salvandoPerfil}
          >
            <Form.Item label="E-mail">
              <Input value={usuario?.email ?? ''} disabled />
            </Form.Item>
            <Form.Item
              name="nome"
              label="Nome"
              rules={[{ required: true, message: 'Informe o nome' }]}
              extra={ehAluno ? 'Nome oficial cadastrado pela escola — para corrigir, procure o bibliotecario(a).' : undefined}
            >
              <Input disabled={ehAluno} />
            </Form.Item>
            {!ehAluno && (
              <Button type="primary" htmlType="submit" loading={salvandoPerfil}>
                Salvar dados
              </Button>
            )}
          </Form>
        </Card>

        <Card title="Alterar senha" style={{ flex: '1 1 360px', maxWidth: 480 }}>
          <Form form={formSenha} layout="vertical" onFinish={salvarSenha} disabled={salvandoSenha}>
            <Form.Item
              name="senhaAtual"
              label="Senha atual"
              rules={[{ required: true, message: 'Informe a senha atual' }]}
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
            <Button type="primary" htmlType="submit" loading={salvandoSenha}>
              Alterar senha
            </Button>
          </Form>
        </Card>
      </div>
    </>
  );
}
