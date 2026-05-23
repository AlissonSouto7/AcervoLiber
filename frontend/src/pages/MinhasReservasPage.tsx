import { useState } from 'react';
import { App, Button, Card, Grid, List, Popconfirm, Space, Table, Tag, Typography, type TableProps } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { cancelarReserva, listarMinhasReservas } from '../api/reservas';
import { mensagemDeErro } from '../api/http';
import type { ReservaResponse, StatusReserva } from '../types/api';
import { formatarData } from '../utils';

const TAMANHO_PAGINA = 10;

const STATUS_INFO: Record<StatusReserva, { cor: string; texto: string }> = {
  PENDENTE: { cor: 'gold', texto: 'Aguardando retirada' },
  CONFIRMADA: { cor: 'green', texto: 'Confirmada' },
  RECUSADA: { cor: 'red', texto: 'Recusada' },
  CANCELADA: { cor: 'default', texto: 'Cancelada' },
  EXPIRADA: { cor: 'default', texto: 'Expirada' },
};

function tagStatus(status: StatusReserva) {
  const info = STATUS_INFO[status];
  return <Tag color={info.cor}>{info.texto}</Tag>;
}

export default function MinhasReservasPage() {
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.md;
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ['minhas-reservas', page],
    queryFn: () => listarMinhasReservas({ page, size: TAMANHO_PAGINA }),
  });

  const cancelar = useMutation({
    mutationFn: (id: number) => cancelarReserva(id),
    onSuccess: () => {
      message.success('Reserva cancelada');
      queryClient.invalidateQueries({ queryKey: ['minhas-reservas'] });
      queryClient.invalidateQueries({ queryKey: ['catalogo'] });
      // Resumo (vagas no limite) muda — sem isso o Catalogo mantem o alert "sem vagas".
      queryClient.invalidateQueries({ queryKey: ['resumo-reservas'] });
    },
    onError: (erro) => message.error(mensagemDeErro(erro)),
  });

  const botaoCancelar = (reserva: ReservaResponse) =>
    reserva.status === 'PENDENTE' ? (
      <Popconfirm
        title="Cancelar esta reserva?"
        okText="Cancelar reserva"
        cancelText="Voltar"
        okButtonProps={{ danger: true }}
        onConfirm={() => cancelar.mutate(reserva.id)}
      >
        <Button size="small" danger>
          Cancelar
        </Button>
      </Popconfirm>
    ) : null;

  const paginacao = {
    current: page + 1,
    pageSize: TAMANHO_PAGINA,
    total: data?.totalElements ?? 0,
    onChange: (p: number) => setPage(p - 1),
  };

  const colunas: TableProps<ReservaResponse>['columns'] = [
    { title: 'Livro', dataIndex: ['livro', 'titulo'] },
    { title: 'Status', key: 'status', width: 190, render: (_, r) => tagStatus(r.status) },
    { title: 'Reservado em', key: 'res', width: 140, render: (_, r) => formatarData(r.dataReserva) },
    { title: 'Validade', key: 'val', width: 140, render: (_, r) => formatarData(r.dataExpiracao) },
    { title: 'Acoes', key: 'acoes', width: 120, render: (_, r) => botaoCancelar(r) },
  ];

  return (
    <>
      <Typography.Title level={3}>Minhas reservas</Typography.Title>

      {isMobile ? (
        <List
          loading={isLoading}
          dataSource={data?.content ?? []}
          locale={{ emptyText: 'Voce ainda nao tem reservas' }}
          pagination={paginacao}
          renderItem={(reserva) => (
            <Card size="small" style={{ marginBottom: 12 }}>
              <Typography.Text strong>{reserva.livro.titulo}</Typography.Text>
              <div style={{ marginTop: 4 }}>
                <Typography.Text type="secondary">
                  Reservado em {formatarData(reserva.dataReserva)} · validade{' '}
                  {formatarData(reserva.dataExpiracao)}
                </Typography.Text>
              </div>
              <Space style={{ marginTop: 8 }}>
                {tagStatus(reserva.status)}
                {botaoCancelar(reserva)}
              </Space>
            </Card>
          )}
        />
      ) : (
        <Table<ReservaResponse>
          rowKey="id"
          loading={isLoading}
          columns={colunas}
          dataSource={data?.content ?? []}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: 'Voce ainda nao tem reservas' }}
          pagination={paginacao}
        />
      )}
    </>
  );
}
