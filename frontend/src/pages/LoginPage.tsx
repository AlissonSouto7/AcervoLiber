import { useState } from 'react';
import { IdcardOutlined, LockOutlined, UserOutlined } from '@ant-design/icons';
import { App, Button, Card, Form, Input, Tabs, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import { login, loginAluno } from '../api/auth';
import { mensagemDeErro } from '../api/http';
import { useAuthStore } from '../auth/authStore';
import type { LoginResponse } from '../types/api';

export default function LoginPage() {
  const navigate = useNavigate();
  const setSessao = useAuthStore((s) => s.setSessao);
  const { message } = App.useApp();
  const [carregando, setCarregando] = useState(false);

  async function entrar(promessa: Promise<LoginResponse>) {
    setCarregando(true);
    try {
      const resp = await promessa;
      setSessao(resp);
      navigate('/');
    } catch (erro) {
      message.error(mensagemDeErro(erro, 'Nao foi possivel entrar'));
    } finally {
      setCarregando(false);
    }
  }

  const formAluno = (
    <Form
      layout="vertical"
      size="large"
      disabled={carregando}
      onFinish={(v: { matricula: string; senha: string }) =>
        entrar(loginAluno(v.matricula.trim(), v.senha))}
    >
      <Form.Item
        name="matricula"
        label="Matricula"
        rules={[{ required: true, message: 'Informe sua matricula' }]}
      >
        <Input prefix={<IdcardOutlined />} placeholder="Sua matricula" autoComplete="username" />
      </Form.Item>
      <Form.Item name="senha" label="Senha" rules={[{ required: true, message: 'Informe a senha' }]}>
        <Input.Password prefix={<LockOutlined />} autoComplete="current-password" />
      </Form.Item>
      <Button type="primary" htmlType="submit" block loading={carregando} size="large">
        Entrar
      </Button>
    </Form>
  );

  const formEquipe = (
    <Form
      layout="vertical"
      size="large"
      disabled={carregando}
      onFinish={(v: { email: string; senha: string }) => entrar(login(v.email.trim(), v.senha))}
    >
      <Form.Item
        name="email"
        label="E-mail"
        rules={[
          { required: true, message: 'Informe o e-mail' },
          { type: 'email', message: 'E-mail invalido' },
        ]}
      >
        <Input prefix={<UserOutlined />} placeholder="voce@escola.com" autoComplete="username" />
      </Form.Item>
      <Form.Item name="senha" label="Senha" rules={[{ required: true, message: 'Informe a senha' }]}>
        <Input.Password prefix={<LockOutlined />} autoComplete="current-password" />
      </Form.Item>
      <Button type="primary" htmlType="submit" block loading={carregando} size="large">
        Entrar
      </Button>
    </Form>
  );

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
      <Card style={{ width: '100%', maxWidth: 400 }}>
        <Typography.Title level={3} style={{ textAlign: 'center', marginBottom: 8 }}>
          AcervoLiber
        </Typography.Title>
        <Tabs
          centered
          defaultActiveKey="aluno"
          items={[
            { key: 'aluno', label: 'Sou aluno', children: formAluno },
            { key: 'equipe', label: 'Sou da equipe', children: formEquipe },
          ]}
        />
      </Card>
    </div>
  );
}
