import { useState } from 'react';
import { Alert, Button, Card, Grid, List, Space, Table, Tag, Typography, type TableProps } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { listarHistorico } from '../api/emprestimos';
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
  const [page, setPage] = useState(0);

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['historico', page],
    queryFn: () => listarHistorico({ page, size: TAMANHO_PAGINA }),
  });

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
