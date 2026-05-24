import { useState } from 'react';
import {
  App,
  Button,
  Card,
  Empty,
  Grid,
  InputNumber,
  Modal,
  Popconfirm,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  type TableProps,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { confirmarReserva, listarReservasPendentes, recusarReserva } from '../api/reservas';
import { mensagemDeErro } from '../api/http';
import type { ReservaResponse } from '../types/api';
import { formatarData } from '../utils';

const PRAZO_PADRAO = 14;
const TAMANHO_PAGINA = 20;

/** Fila de reservas pendentes — bibliotecario confirma (vira emprestimo) ou recusa. */
export default function ReservasPendentesPage() {
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.md;
  const { message } = App.useApp();
  const queryClient = useQueryClient();

  // Reserva escolhida para confirmar — abre o modal de prazo.
  const [confirmando, setConfirmando] = useState<ReservaResponse | null>(null);
  const [prazoDias, setPrazoDias] = useState<number>(PRAZO_PADRAO);
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ['reservas-pendentes', page],
    queryFn: () => listarReservasPendentes({ page, size: TAMANHO_PAGINA }),
    // Polling: a fila e viva (alunos reservam a qualquer momento); o bibliotecario
    // ve novas reservas sem precisar dar F5.
    refetchInterval: 30_000,
  });

  function invalidarTudo() {
    queryClient.invalidateQueries({ queryKey: ['reservas-pendentes'] });
    queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    queryClient.invalidateQueries({ queryKey: ['livros'] });
    queryClient.invalidateQueries({ queryKey: ['emprestimos-ativos'] });
    // Lado aluno (sessoes no mesmo navegador / PC compartilhado): a reserva sai
    // de "minhas reservas pendentes", o resumo de vagas muda, o catalogo mostra
    // o livro como emprestado.
    queryClient.invalidateQueries({ queryKey: ['minhas-reservas'] });
    queryClient.invalidateQueries({ queryKey: ['resumo-reservas'] });
    queryClient.invalidateQueries({ queryKey: ['catalogo'] });
    queryClient.invalidateQueries({ queryKey: ['emprestimos'] });
  }

  const confirmar = useMutation({
    mutationFn: ({ id, prazo }: { id: number; prazo: number }) => confirmarReserva(id, prazo),
    onSuccess: () => {
      message.success('Reserva confirmada — empréstimo registrado.');
      setConfirmando(null);
      invalidarTudo();
    },
    onError: (erro) => message.error(mensagemDeErro(erro)),
  });

  const recusar = useMutation({
    mutationFn: (id: number) => recusarReserva(id),
    onSuccess: () => {
      message.success('Reserva recusada.');
      invalidarTudo();
    },
    onError: (erro) => message.error(mensagemDeErro(erro)),
  });

  function abrirConfirmacao(reserva: ReservaResponse) {
    setPrazoDias(PRAZO_PADRAO);
    setConfirmando(reserva);
  }

  const acoes = (reserva: ReservaResponse) => (
    <Space wrap>
      <Button
        type="primary"
        size="small"
        loading={confirmar.isPending && confirmar.variables?.id === reserva.id}
        onClick={() => abrirConfirmacao(reserva)}
      >
        Confirmar
      </Button>
      <Popconfirm
        title="Recusar esta reserva?"
        description="O exemplar volta para o acervo."
        okText="Recusar"
        cancelText="Voltar"
        okButtonProps={{ danger: true }}
        onConfirm={() => recusar.mutate(reserva.id)}
      >
        <Button
          size="small"
          danger
          loading={recusar.isPending && recusar.variables === reserva.id}
        >
          Recusar
        </Button>
      </Popconfirm>
    </Space>
  );

  const colunas: TableProps<ReservaResponse>['columns'] = [
    { title: 'Livro', key: 'livro', render: (_, r) => r.livro.titulo },
    {
      title: 'Aluno',
      key: 'aluno',
      render: (_, r) => (
        <>
          {r.aluno.nome}
          <br />
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            {r.aluno.matricula} · {r.aluno.turma}
          </Typography.Text>
        </>
      ),
    },
    { title: 'Reservado em', key: 'res', width: 140, render: (_, r) => formatarData(r.dataReserva) },
    { title: 'Validade', key: 'val', width: 140, render: (_, r) => formatarData(r.dataExpiracao) },
    { title: 'Ações', key: 'acoes', width: 200, render: (_, r) => acoes(r) },
  ];

  const reservas = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;

  return (
    <>
      <Typography.Title level={3}>Reservas pendentes</Typography.Title>
      <Typography.Paragraph type="secondary">
        Confirme para transformar a reserva em empréstimo, ou recuse para devolver o exemplar ao
        acervo.
      </Typography.Paragraph>

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin />
        </div>
      ) : reservas.length === 0 ? (
        <Empty description="Nenhuma reserva aguardando" />
      ) : isMobile ? (
        reservas.map((reserva) => (
          <Card key={reserva.id} size="small" style={{ marginBottom: 12 }}>
            <Typography.Text strong>{reserva.livro.titulo}</Typography.Text>
            <div style={{ marginTop: 4 }}>
              <Typography.Text>{reserva.aluno.nome}</Typography.Text>
              <br />
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                {reserva.aluno.matricula} · {reserva.aluno.turma}
              </Typography.Text>
            </div>
            <div style={{ marginTop: 4 }}>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                Reservado em {formatarData(reserva.dataReserva)} · validade{' '}
                {formatarData(reserva.dataExpiracao)}
              </Typography.Text>
            </div>
            <div style={{ marginTop: 8 }}>{acoes(reserva)}</div>
          </Card>
        ))
      ) : (
        <Table<ReservaResponse>
          rowKey="id"
          columns={colunas}
          dataSource={reservas}
          scroll={{ x: 'max-content' }}
          pagination={{
            current: page + 1,
            pageSize: TAMANHO_PAGINA,
            total: totalElements,
            onChange: (p) => setPage(p - 1),
            showSizeChanger: false,
          }}
        />
      )}

      <Modal
        title="Confirmar reserva"
        open={confirmando !== null}
        onCancel={() => setConfirmando(null)}
        confirmLoading={confirmar.isPending}
        okText="Confirmar empréstimo"
        cancelText="Cancelar"
        onOk={() => {
          if (confirmando) confirmar.mutate({ id: confirmando.id, prazo: prazoDias });
        }}
      >
        {confirmando && (
          <>
            <Typography.Paragraph style={{ marginBottom: 4 }}>
              <Tag color="blue">{confirmando.livro.titulo}</Tag>
            </Typography.Paragraph>
            <Typography.Paragraph type="secondary">
              Aluno: {confirmando.aluno.nome} ({confirmando.aluno.matricula})
            </Typography.Paragraph>
            <Typography.Text>Prazo de empréstimo (dias):</Typography.Text>
            <div style={{ marginTop: 8 }}>
              <InputNumber
                min={1}
                max={90}
                value={prazoDias}
                onChange={(v) => setPrazoDias(v ?? PRAZO_PADRAO)}
                style={{ width: '100%' }}
              />
            </div>
          </>
        )}
      </Modal>
    </>
  );
}
