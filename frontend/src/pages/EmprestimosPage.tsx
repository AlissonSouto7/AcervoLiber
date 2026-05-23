import { useState } from 'react';
import { PlusOutlined } from '@ant-design/icons';
import {
  App,
  Button,
  Card,
  Drawer,
  Form,
  Grid,
  InputNumber,
  List,
  Popconfirm,
  Select,
  Table,
  Typography,
  type TableProps,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { listarAlunos } from '../api/alunos';
import { listarLivros } from '../api/livros';
import {
  devolverEmprestimo,
  listarEmprestimosAtivos,
  registrarEmprestimo,
  type EmprestimoPayload,
} from '../api/emprestimos';
import { mensagemDeErro } from '../api/http';
import { StatusUrgenciaTag } from '../components/StatusUrgenciaTag';
import type { EmprestimoResponse } from '../types/api';
import { formatarData } from '../utils';

// Apos registrar/devolver: alunos (livrosEmprestadosAtualmente), reservas (resumo
// de vagas) e reservas-resumo tambem mudam — invalidar tudo evita UI stale.
const CHAVES_RELACIONADAS = [
  'emprestimos-ativos', 'dashboard', 'livros', 'livros-opcoes', 'historico',
  'alunos', 'alunos-opcoes', 'reservas', 'resumo-reservas',
];

export default function EmprestimosPage() {
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.md;
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [drawerAberto, setDrawerAberto] = useState(false);

  const { data: ativos, isLoading } = useQuery({
    queryKey: ['emprestimos-ativos'],
    queryFn: listarEmprestimosAtivos,
  });

  // Opcoes do formulario — carregadas apenas quando o drawer abre
  const { data: livros } = useQuery({
    queryKey: ['livros-opcoes'],
    queryFn: () => listarLivros({ size: 100 }),
    enabled: drawerAberto,
  });
  const { data: alunos } = useQuery({
    queryKey: ['alunos-opcoes'],
    queryFn: () => listarAlunos({ size: 100 }),
    enabled: drawerAberto,
  });

  function invalidarRelacionadas() {
    CHAVES_RELACIONADAS.forEach((chave) =>
      queryClient.invalidateQueries({ queryKey: [chave] }),
    );
  }

  const registrar = useMutation({
    mutationFn: (payload: EmprestimoPayload) => registrarEmprestimo(payload),
    onSuccess: () => {
      message.success('Emprestimo registrado');
      invalidarRelacionadas();
      setDrawerAberto(false);
    },
    onError: (erro) => message.error(mensagemDeErro(erro)),
  });

  const devolver = useMutation({
    mutationFn: (id: number) => devolverEmprestimo(id),
    onSuccess: () => {
      message.success('Devolucao registrada');
      invalidarRelacionadas();
    },
    onError: (erro) => message.error(mensagemDeErro(erro)),
  });

  const livroOpcoes = (livros?.content ?? []).map((l) => ({
    value: l.id,
    label: `${l.titulo} — ${l.quantidadeDisponivel} disp.`,
    disabled: l.quantidadeDisponivel === 0,
  }));

  const alunoOpcoes = (alunos?.content ?? []).map((a) => ({
    value: a.id,
    label: `${a.nome} — ${a.turma}`,
  }));

  const botaoDevolver = (emp: EmprestimoResponse) => (
    <Popconfirm
      title="Confirmar devolucao deste livro?"
      okText="Devolver"
      cancelText="Cancelar"
      onConfirm={() => devolver.mutate(emp.id)}
    >
      <Button size="small" type="primary">
        Devolver
      </Button>
    </Popconfirm>
  );

  const colunas: TableProps<EmprestimoResponse>['columns'] = [
    { title: 'Livro', dataIndex: ['livro', 'titulo'] },
    { title: 'Aluno', dataIndex: ['aluno', 'nome'] },
    { title: 'Turma', dataIndex: ['aluno', 'turma'], width: 90 },
    {
      title: 'Devolver ate',
      key: 'prazo',
      width: 130,
      render: (_, e) => formatarData(e.dataDevolucaoPrevista),
    },
    {
      title: 'Status',
      key: 'status',
      width: 180,
      render: (_, e) => <StatusUrgenciaTag status={e.statusUrgencia} />,
    },
    { title: 'Acoes', key: 'acoes', width: 120, render: (_, e) => botaoDevolver(e) },
  ];

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
          Emprestimos ativos
        </Typography.Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setDrawerAberto(true)}>
          Novo emprestimo
        </Button>
      </div>

      {isMobile ? (
        <List
          loading={isLoading}
          dataSource={ativos ?? []}
          locale={{ emptyText: 'Nenhum emprestimo ativo' }}
          pagination={{ pageSize: 8, hideOnSinglePage: true }}
          renderItem={(emp) => (
            <Card size="small" style={{ marginBottom: 12 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                <div style={{ minWidth: 0 }}>
                  <Typography.Text strong>{emp.livro.titulo}</Typography.Text>
                  <div>
                    <Typography.Text type="secondary">
                      {emp.aluno.nome} · Turma {emp.aluno.turma}
                    </Typography.Text>
                  </div>
                  <div>
                    <Typography.Text type="secondary">
                      Devolver ate {formatarData(emp.dataDevolucaoPrevista)}
                    </Typography.Text>
                  </div>
                  <div style={{ marginTop: 8 }}>
                    <StatusUrgenciaTag status={emp.statusUrgencia} />
                  </div>
                </div>
                {botaoDevolver(emp)}
              </div>
            </Card>
          )}
        />
      ) : (
        <Table<EmprestimoResponse>
          rowKey="id"
          loading={isLoading}
          columns={colunas}
          dataSource={ativos ?? []}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: 'Nenhum emprestimo ativo' }}
          pagination={{ pageSize: 10, hideOnSinglePage: true }}
        />
      )}

      <Drawer
        title="Novo emprestimo"
        open={drawerAberto}
        onClose={() => setDrawerAberto(false)}
        width={isMobile ? '100%' : 420}
      >
        {drawerAberto && (
          <Form<EmprestimoPayload>
            layout="vertical"
            initialValues={{ prazoDias: 7 }}
            onFinish={(valores) => registrar.mutate(valores)}
          >
            <Form.Item
              name="livroId"
              label="Livro"
              rules={[{ required: true, message: 'Selecione o livro' }]}
            >
              <Select
                showSearch
                placeholder="Buscar livro"
                optionFilterProp="label"
                options={livroOpcoes}
                loading={!livros}
              />
            </Form.Item>
            <Form.Item
              name="alunoId"
              label="Aluno"
              rules={[{ required: true, message: 'Selecione o aluno' }]}
            >
              <Select
                showSearch
                placeholder="Buscar aluno"
                optionFilterProp="label"
                options={alunoOpcoes}
                loading={!alunos}
              />
            </Form.Item>
            <Form.Item
              name="prazoDias"
              label="Prazo de devolucao (dias)"
              rules={[{ required: true, message: 'Informe o prazo' }]}
            >
              <InputNumber style={{ width: '100%' }} min={1} max={30} />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={registrar.isPending}>
              Registrar emprestimo
            </Button>
          </Form>
        )}
      </Drawer>
    </>
  );
}
