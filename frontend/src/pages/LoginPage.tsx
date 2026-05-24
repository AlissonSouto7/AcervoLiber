import { useState } from 'react';
import { IdcardOutlined, LockOutlined, UserAddOutlined, UserOutlined } from '@ant-design/icons';
import { App, Button, Card, Form, Input, Tabs, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import { login, loginAluno, registerAluno } from '../api/auth';
import { mensagemDeErro } from '../api/http';
import { useAuthStore } from '../auth/authStore';
import type { LoginResponse } from '../types/api';

type ModoAluno = 'login' | 'cadastro';

export default function LoginPage() {
  const navigate = useNavigate();
  const setSessao = useAuthStore((s) => s.setSessao);
  const { message } = App.useApp();
  const [carregando, setCarregando] = useState(false);
  const [modoAluno, setModoAluno] = useState<ModoAluno>('login');

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

  async function cadastrarAluno(v: {
    matricula: string;
    nome: string;
    senha: string;
    confirmacao: string;
  }) {
    if (v.senha !== v.confirmacao) {
      message.error('As senhas nao conferem.');
      return;
    }
    setCarregando(true);
    try {
      await registerAluno(v.matricula.trim(), v.nome.trim(), v.senha);
      message.success('Cadastro feito! Fazendo login...');
      // Auto-login com a senha que o aluno acabou de definir.
      await entrar(loginAluno(v.matricula.trim(), v.senha));
    } catch (erro) {
      message.error(mensagemDeErro(erro, 'Nao foi possivel cadastrar'));
      setCarregando(false);
    }
  }

  const formAlunoLogin = (
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
      <Button
        type="link"
        block
        style={{ marginTop: 8 }}
        onClick={() => setModoAluno('cadastro')}
        icon={<UserAddOutlined />}
      >
        Primeiro acesso? Cadastrar
      </Button>
    </Form>
  );

  const formAlunoCadastro = (
    <Form layout="vertical" size="large" disabled={carregando} onFinish={cadastrarAluno}>
      <Typography.Paragraph type="secondary" style={{ fontSize: 13 }}>
        Use a <strong>matricula</strong> e o <strong>nome completo</strong> conforme cadastrados pela
        escola. Se os dados nao conferirem, procure o(a) bibliotecario(a).
      </Typography.Paragraph>
      <Form.Item
        name="matricula"
        label="Matricula"
        rules={[{ required: true, message: 'Informe sua matricula' }]}
      >
        <Input prefix={<IdcardOutlined />} placeholder="Ex: 2026001" autoComplete="off" />
      </Form.Item>
      <Form.Item
        name="nome"
        label="Nome completo"
        rules={[
          { required: true, message: 'Informe seu nome completo' },
          { min: 3, message: 'Nome muito curto' },
        ]}
      >
        <Input prefix={<UserOutlined />} placeholder="Como cadastrado pela escola" autoComplete="off" />
      </Form.Item>
      <Form.Item
        name="senha"
        label="Senha"
        rules={[
          { required: true, message: 'Informe uma senha' },
          { min: 8, message: 'Minimo 8 caracteres' },
        ]}
        extra="Min. 8 caracteres com letras e numeros."
      >
        <Input.Password prefix={<LockOutlined />} autoComplete="new-password" />
      </Form.Item>
      <Form.Item
        name="confirmacao"
        label="Confirmar senha"
        rules={[{ required: true, message: 'Confirme a senha' }]}
      >
        <Input.Password prefix={<LockOutlined />} autoComplete="new-password" />
      </Form.Item>
      <Button type="primary" htmlType="submit" block loading={carregando} size="large">
        Cadastrar e entrar
      </Button>
      <Button
        type="link"
        block
        style={{ marginTop: 8 }}
        onClick={() => setModoAluno('login')}
      >
        Ja tem cadastro? Fazer login
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
          onChange={() => setModoAluno('login')}
          items={[
            {
              key: 'aluno',
              label: 'Sou aluno',
              children: modoAluno === 'login' ? formAlunoLogin : formAlunoCadastro,
            },
            { key: 'equipe', label: 'Sou da equipe', children: formEquipe },
          ]}
        />
      </Card>
    </div>
  );
}
