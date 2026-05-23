import { useState } from 'react';
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
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
  Space,
  Table,
  Tag,
  Typography,
  type TableProps,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  atualizarAluno,
  criarAluno,
  listarAlunos,
  removerAluno,
  type AlunoPayload,
} from '../api/alunos';
import { mensagemDeErro } from '../api/http';
import type { AlunoResponse } from '../types/api';

const TAMANHO_PAGINA = 10;

function tagEmprestimos(aluno: AlunoResponse) {
  const n = aluno.livrosEmprestadosAtualmente;
  return <Tag color={n > 0 ? 'blue' : 'default'}>{n} livro(s) emprestado(s)</Tag>;
}

export default function AlunosPage() {
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.md;
  const { message } = App.useApp();
  const queryClient = useQueryClient();

  const [termo, setTermo] = useState('');
  const [page, setPage] = useState(0);
  const [drawerAberto, setDrawerAberto] = useState(false);
  const [editando, setEditando] = useState<AlunoResponse | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['alunos', termo, page],
    queryFn: () => listarAlunos({ termo, page, size: TAMANHO_PAGINA }),
  });

  const salvar = useMutation({
    mutationFn: (valores: AlunoPayload) =>
      editando ? atualizarAluno(editando.id, valores) : criarAluno(valores),
    onSuccess: () => {
      message.success(editando ? 'Aluno atualizado' : 'Aluno cadastrado');
      queryClient.invalidateQueries({ queryKey: ['alunos'] });
      fecharDrawer();
    },
    onError: (erro) => message.error(mensagemDeErro(erro)),
  });

  const remover = useMutation({
    mutationFn: (id: number) => removerAluno(id),
    onSuccess: () => {
      message.success('Aluno removido');
      queryClient.invalidateQueries({ queryKey: ['alunos'] });
    },
    onError: (erro) => message.error(mensagemDeErro(erro)),
  });

  function abrirNovo() {
    setEditando(null);
    setDrawerAberto(true);
  }

  function abrirEdicao(aluno: AlunoResponse) {
    setEditando(aluno);
    setDrawerAberto(true);
  }

  function fecharDrawer() {
    setDrawerAberto(false);
    setEditando(null);
  }

  const valoresIniciais: Partial<AlunoPayload> = editando
    ? { matricula: editando.matricula, nome: editando.nome, turma: editando.turma }
    : {};

  const acoes = (aluno: AlunoResponse) => (
    <Space>
      <Button size="small" icon={<EditOutlined />} onClick={() => abrirEdicao(aluno)} />
      <Popconfirm
        title="Remover este aluno?"
        description="So e possivel remover alunos sem historico de emprestimos."
        okText="Remover"
        cancelText="Cancelar"
        okButtonProps={{ danger: true }}
        onConfirm={() => remover.mutate(aluno.id)}
      >
        <Button size="small" danger icon={<DeleteOutlined />} />
      </Popconfirm>
    </Space>
  );

  const colunas: TableProps<AlunoResponse>['columns'] = [
    { title: 'Matricula', dataIndex: 'matricula', width: 120 },
    { title: 'Nome', dataIndex: 'nome' },
    { title: 'Turma', dataIndex: 'turma', width: 90 },
    { title: 'Emprestimos', key: 'emp', width: 200, render: (_, a) => tagEmprestimos(a) },
    { title: 'Acoes', key: 'acoes', width: 110, render: (_, a) => acoes(a) },
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
          Alunos
        </Typography.Title>
        <Space wrap style={{ flex: isMobile ? '1 1 100%' : undefined }}>
          <Input.Search
            placeholder="Buscar por nome, matricula ou turma"
            allowClear
            onSearch={(v) => {
              setTermo(v);
              setPage(0);
            }}
            style={{ width: isMobile ? '100%' : 280 }}
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={abrirNovo}>
            Novo aluno
          </Button>
        </Space>
      </div>

      {isMobile ? (
        <List
          loading={isLoading}
          dataSource={data?.content ?? []}
          locale={{ emptyText: 'Nenhum aluno encontrado' }}
          pagination={paginacao}
          renderItem={(aluno) => (
            <Card size="small" style={{ marginBottom: 12 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                <div style={{ minWidth: 0 }}>
                  <Typography.Text strong>{aluno.nome}</Typography.Text>
                  <div>
                    <Typography.Text type="secondary">
                      {aluno.matricula} · Turma {aluno.turma}
                    </Typography.Text>
                  </div>
                  <div style={{ marginTop: 8 }}>{tagEmprestimos(aluno)}</div>
                </div>
                {acoes(aluno)}
              </div>
            </Card>
          )}
        />
      ) : (
        <Table<AlunoResponse>
          rowKey="id"
          loading={isLoading}
          columns={colunas}
          dataSource={data?.content ?? []}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: 'Nenhum aluno encontrado' }}
          pagination={paginacao}
        />
      )}

      <Drawer
        title={editando ? 'Editar aluno' : 'Novo aluno'}
        open={drawerAberto}
        onClose={fecharDrawer}
        width={isMobile ? '100%' : 420}
      >
        {drawerAberto && (
          <Form<AlunoPayload>
            layout="vertical"
            initialValues={valoresIniciais}
            onFinish={(valores) => salvar.mutate(valores)}
          >
            <Form.Item
              name="matricula"
              label="Matricula"
              rules={[{ required: true, message: 'Informe a matricula' }]}
            >
              <Input />
            </Form.Item>
            <Form.Item
              name="nome"
              label="Nome completo"
              rules={[{ required: true, message: 'Informe o nome' }]}
            >
              <Input />
            </Form.Item>
            <Form.Item
              name="turma"
              label="Turma"
              rules={[{ required: true, message: 'Informe a turma' }]}
            >
              <Input placeholder="Ex.: 9A" />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={salvar.isPending}>
              Salvar
            </Button>
          </Form>
        )}
      </Drawer>
    </>
  );
}
