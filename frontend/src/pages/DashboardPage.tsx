import { useQuery } from '@tanstack/react-query';
import { Alert, Card, Col, Row, Spin, Statistic, Table, Typography } from 'antd';
import { getDashboard } from '../api/dashboard';
import type { DashboardAlertaDTO } from '../types/api';

export default function DashboardPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['dashboard'],
    queryFn: getDashboard,
  });

  if (isLoading) {
    return <Spin size="large" style={{ display: 'block', marginTop: 80 }} />;
  }
  if (isError || !data) {
    return <Alert type="error" message="Erro ao carregar o dashboard" showIcon />;
  }

  return (
    <>
      <Typography.Title level={3}>Dashboard</Typography.Title>

      <Row gutter={[16, 16]}>
        <Col xs={12} md={6}>
          <Card>
            <Statistic title="Livros" value={data.totais.totalLivros} />
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card>
            <Statistic title="Alunos" value={data.totais.totalAlunos} />
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card>
            <Statistic title="Empréstimos ativos" value={data.totais.emprestimosAtivos} />
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card>
            <Statistic
              title="Atrasados"
              value={data.totais.emprestimosAtrasados}
              valueStyle={{ color: data.totais.emprestimosAtrasados > 0 ? '#cf1322' : undefined }}
            />
          </Card>
        </Col>
      </Row>

      <Card title="Livros mais emprestados" style={{ marginTop: 16 }}>
        <Table
          rowKey="livroId"
          dataSource={data.livrosMaisEmprestados}
          pagination={false}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: 'Sem dados ainda' }}
          columns={[
            { title: 'Título', dataIndex: 'titulo' },
            { title: 'Autor', dataIndex: 'autor' },
            { title: 'Empréstimos', dataIndex: 'totalEmprestimos', width: 140 },
          ]}
        />
      </Card>

      <Card title="Devoluções atrasadas" style={{ marginTop: 16 }}>
        <Table<DashboardAlertaDTO>
          rowKey="emprestimoId"
          dataSource={data.alertasAtrasados}
          pagination={false}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: 'Nenhum empréstimo atrasado' }}
          columns={[
            { title: 'Livro', dataIndex: 'livroTitulo' },
            { title: 'Aluno', dataIndex: 'alunoNome' },
            // Matricula mascarada (Fase 7 — reduz exposicao de identificador
            // unico de menor quando a tela fica visivel a terceiros).
            { title: 'Matrícula', dataIndex: 'alunoMatriculaMascarada', width: 130 },
            { title: 'Devolução prevista', dataIndex: 'dataDevolucaoPrevista', width: 180 },
          ]}
        />
      </Card>
    </>
  );
}
