import { useState } from 'react';
import { DeleteOutlined } from '@ant-design/icons';
import {
  Alert,
  App,
  Button,
  Card,
  Grid,
  List,
  Popconfirm,
  Space,
  Table,
  Tag,
  Typography,
  type TableProps,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { listarHistorico, removerEmprestimoDoHistorico } from '../api/emprestimos';
import { mensagemDeErro } from '../api/http';
import { StatusUrgenciaTag } from '../components/StatusUrgenciaTag';
import type { EmprestimoResponse } from '../types/api';
import { formatarData } from '../utils';

const TAMANHO_PAGINA = 10;

function tagSituacao(emp: EmprestimoResponse) {
  return emp.situacao === 'DEVOLVIDO' ? (
    <Tag color="default">Devolvido</Tag>
  ) : (
    <Tag color="processing">Ativo</Tag>
  );
}

export default function HistoricoPage() {
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.md;
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['historico', page],
    queryFn: () => listarHistorico({ page, size: TAMANHO_PAGINA }),
  });

  const remover = useMutation({
    mutationFn: (id: number) => removerEmprestimoDoHistorico(id),
    onSuccess: () => {
      message.success('Registro removido do histórico');
      queryClient.invalidateQueries({ queryKey: ['historico'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
    onError: (e) => message.error(mensagemDeErro(e)),
  });

  const botaoRemover = (emp: EmprestimoResponse) => {
    const podeRemover = emp.situacao !== 'ATIVO';
    return (
      <Popconfirm
        title="Remover este registro do histórico?"
        description="Esta ação é permanente. O exemplar e os dados do aluno permanecem — só o registro deste empréstimo é apagado."
        okText="Remover"
        okButtonProps={{ danger: true }}
        onConfirm={() => remover.mutate(emp.id)}
        disabled={!podeRemover}
      >
        <Button
          size="small"
          danger
          icon={<DeleteOutlined />}
          disabled={!podeRemover}
          loading={remover.isPending && remover.variables === emp.id}
          title={podeRemover ? 'Remover do histórico' : 'Empréstimo ativo não pode ser removido'}
        />
      </Popconfirm>
    );
  };

  const colunas: TableProps<EmprestimoResponse>['columns'] = [
    { title: 'Livro', dataIndex: ['livro', 'titulo'] },
    { title: 'Aluno', dataIndex: ['aluno', 'nome'] },
    {
      title: 'Empréstimo',
      key: 'de',
      width: 120,
      render: (_, e) => formatarData(e.dataEmprestimo),
    },
    {
      title: 'Devolução prevista',
      key: 'prev',
      width: 160,
      render: (_, e) => formatarData(e.dataDevolucaoPrevista),
    },
    {
      title: 'Devolvido em',
      key: 'efet',
      width: 130,
      render: (_, e) => formatarData(e.dataDevolucaoEfetiva),
    },
    { title: 'Situação', key: 'sit', width: 110, render: (_, e) => tagSituacao(e) },
    {
      title: 'Status',
      key: 'status',
      width: 180,
      render: (_, e) => <StatusUrgenciaTag status={e.statusUrgencia} />,
    },
    { title: '', key: 'acoes', width: 70, render: (_, e) => botaoRemover(e) },
  ];

  const paginacao = {
    current: page + 1,
    pageSize: TAMANHO_PAGINA,
    total: data?.totalElements ?? 0,
    onChange: (p: number) => setPage(p - 1),
  };

  return (
    <>
      <Typography.Title level={3}>Histórico de empréstimos</Typography.Title>

      {isError && (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
          message="Não foi possível carregar o histórico"
          description={mensagemDeErro(error)}
          action={
            <Button size="small" onClick={() => refetch()}>
              Tentar novamente
            </Button>
          }
        />
      )}

      {isMobile ? (
        <List
          loading={isLoading}
          dataSource={data?.content ?? []}
          locale={{ emptyText: 'Nenhum empréstimo no histórico' }}
          pagination={paginacao}
          renderItem={(emp) => (
            <Card size="small" style={{ marginBottom: 12 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                <div style={{ minWidth: 0, flex: 1 }}>
                  <Typography.Text strong>{emp.livro.titulo}</Typography.Text>
                  <div>
                    <Typography.Text type="secondary">
                      {emp.aluno.nome} · Turma {emp.aluno.turma}
                    </Typography.Text>
                  </div>
                  <div>
                    <Typography.Text type="secondary">
                      {formatarData(emp.dataEmprestimo)} → {formatarData(emp.dataDevolucaoPrevista)}
                      {emp.dataDevolucaoEfetiva
                        ? ` · devolvido ${formatarData(emp.dataDevolucaoEfetiva)}`
                        : ''}
                    </Typography.Text>
                  </div>
                  <Space style={{ marginTop: 8 }}>
                    {tagSituacao(emp)}
                    <StatusUrgenciaTag status={emp.statusUrgencia} />
                  </Space>
                </div>
                {botaoRemover(emp)}
              </div>
            </Card>
          )}
        />
      ) : (
        <Table<EmprestimoResponse>
          rowKey="id"
          loading={isLoading}
          columns={colunas}
          dataSource={data?.content ?? []}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: 'Nenhum empréstimo no histórico' }}
          pagination={paginacao}
        />
      )}
    </>
  );
}
