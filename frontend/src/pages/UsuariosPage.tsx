import { useState } from 'react';
import { PlusOutlined } from '@ant-design/icons';
import {
  App,
  Button,
  Card,
  Drawer,
  Form,
  Grid,
  Input,
  List,
  Popconfirm,
  Select,
  Switch,
  Table,
  Tag,
  Typography,
  type TableProps,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  alterarStatusUsuario,
  criarUsuario,
  listarUsuarios,
  type CriarUsuarioPayload,
} from '../api/usuarios';
import { mensagemDeErro } from '../api/http';
import { useAuthStore } from '../auth/authStore';
import type { Role, Usuario } from '../types/api';

const TAMANHO_PAGINA = 10;

function tagRole(role: Role) {
  return <Tag color={role === 'ADMIN' ? 'gold' : 'blue'}>{role}</Tag>;
}

export default function UsuariosPage() {
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.md;
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const usuarioLogado = useAuthStore((s) => s.usuario);

  const [page, setPage] = useState(0);
  const [drawerAberto, setDrawerAberto] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ['usuarios', page],
    queryFn: () => listarUsuarios({ page, size: TAMANHO_PAGINA }),
  });

  const criar = useMutation({
    mutationFn: (payload: CriarUsuarioPayload) => criarUsuario(payload),
    onSuccess: () => {
      message.success('Usuario criado');
      queryClient.invalidateQueries({ queryKey: ['usuarios'] });
      setDrawerAberto(false);
    },
    onError: (erro) => message.error(mensagemDeErro(erro)),
  });

  const alterarStatus = useMutation({
    mutationFn: ({ id, ativo }: { id: number; ativo: boolean }) => alterarStatusUsuario(id, ativo),
    onSuccess: () => {
      message.success('Status atualizado');
      queryClient.invalidateQueries({ queryKey: ['usuarios'] });
    },
    onError: (erro) => message.error(mensagemDeErro(erro)),
  });

  const switchStatus = (u: Usuario) => {
    const ehSelf = u.id === usuarioLogado?.id;
    const sw = (
      <Switch
        checked={u.ativo}
        loading={alterarStatus.isPending}
        // Impede o admin de desativar a si mesmo (backend tambem bloqueia)
        disabled={ehSelf || alterarStatus.isPending}
        // Ativar -> direto. Desativar -> Popconfirm (encerra sessoes do alvo).
        onChange={(ativo) => {
          if (ativo) alterarStatus.mutate({ id: u.id, ativo: true });
        }}
      />
    );
    // Quando esta ATIVO, clicar pede confirmacao (vai desativar).
    return u.ativo ? (
      <Popconfirm
        title={`Desativar ${u.nome}?`}
        description="Todas as sessoes ativas dele serao encerradas imediatamente."
        okText="Desativar"
        okButtonProps={{ danger: true }}
        cancelText="Cancelar"
        onConfirm={() => alterarStatus.mutate({ id: u.id, ativo: false })}
        disabled={ehSelf}
      >
        {sw}
      </Popconfirm>
    ) : (
      sw
    );
  };

  const colunas: TableProps<Usuario>['columns'] = [
    { title: 'Nome', dataIndex: 'nome' },
    { title: 'E-mail', dataIndex: 'email' },
    { title: 'Perfil', dataIndex: 'role', width: 150, render: (r: Role) => tagRole(r) },
    { title: 'Ativo', key: 'ativo', width: 90, render: (_, u) => switchStatus(u) },
  ];

  const paginacao = {
    current: page + 1,
    pageSize: TAMANHO_PAGINA,
    total: data?.totalElements ?? 0,
    onChange: (p: number) => setPage(p - 1),
  };

  return (
    <>
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: 12,
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <Typography.Title level={3} style={{ margin: 0 }}>
          Usuarios
        </Typography.Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setDrawerAberto(true)}>
          Novo usuario
        </Button>
      </div>

      {isMobile ? (
        <List
          loading={isLoading}
          dataSource={data?.content ?? []}
          locale={{ emptyText: 'Nenhum usuario' }}
          pagination={paginacao}
          renderItem={(u) => (
            <Card size="small" style={{ marginBottom: 12 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                <div style={{ minWidth: 0 }}>
                  <Typography.Text strong>{u.nome}</Typography.Text>
                  <div>
                    <Typography.Text type="secondary">{u.email}</Typography.Text>
                  </div>
                  <div style={{ marginTop: 8 }}>{tagRole(u.role)}</div>
                </div>
                {switchStatus(u)}
              </div>
            </Card>
          )}
        />
      ) : (
        <Table<Usuario>
          rowKey="id"
          loading={isLoading}
          columns={colunas}
          dataSource={data?.content ?? []}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: 'Nenhum usuario' }}
          pagination={paginacao}
        />
      )}

      <Drawer
        title="Novo usuario"
        open={drawerAberto}
        onClose={() => setDrawerAberto(false)}
        width={isMobile ? '100%' : 420}
      >
        {drawerAberto && (
          <Form<CriarUsuarioPayload>
            layout="vertical"
            initialValues={{ role: 'BIBLIOTECARIO' }}
            onFinish={(valores) => criar.mutate(valores)}
          >
            <Form.Item
              name="nome"
              label="Nome completo"
              rules={[{ required: true, message: 'Informe o nome' }]}
            >
              <Input />
            </Form.Item>
            <Form.Item
              name="email"
              label="E-mail"
              rules={[
                { required: true, message: 'Informe o e-mail' },
                { type: 'email', message: 'E-mail invalido' },
              ]}
            >
              <Input />
            </Form.Item>
            <Form.Item
              name="senha"
              label="Senha"
              rules={[
                { required: true, message: 'Informe a senha' },
                { min: 10, message: 'A senha deve ter ao menos 10 caracteres' },
              ]}
            >
              <Input.Password />
            </Form.Item>
            <Form.Item
              name="role"
              label="Perfil"
              rules={[{ required: true, message: 'Selecione o perfil' }]}
            >
              <Select
                options={[
                  { value: 'BIBLIOTECARIO', label: 'Bibliotecario' },
                  { value: 'ADMIN', label: 'Administrador' },
                ]}
              />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={criar.isPending}>
              Criar usuario
            </Button>
          </Form>
        )}
      </Drawer>
    </>
  );
}
