import { useState } from 'react';
import { DeleteOutlined, EditOutlined, PlusOutlined, WarningOutlined, ReloadOutlined } from '@ant-design/icons';
import {
  App,
  Button,
  Empty,
  Input,
  Popconfirm,
  Skeleton,
  Space,
  Tag,
  Typography,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  adicionarExemplar,
  listarExemplares,
  marcarExtraviado,
  reativarExemplar,
  removerExemplar,
  renomearExemplar,
} from '../api/exemplares';
import { mensagemDeErro } from '../api/http';
import type { ExemplarResponse, SituacaoExemplar } from '../types/api';

interface Props {
  livroId: number;
}

const COR_SITUACAO: Record<SituacaoExemplar, string> = {
  DISPONIVEL: 'green',
  EMPRESTADO: 'blue',
  RESERVADO: 'gold',
  EXTRAVIADO: 'red',
};

const LABEL_SITUACAO: Record<SituacaoExemplar, string> = {
  DISPONIVEL: 'Disponível',
  EMPRESTADO: 'Emprestado',
  RESERVADO: 'Reservado',
  EXTRAVIADO: 'Extraviado',
};

/**
 * Gerencia exemplares (cópias físicas) de um livro: lista, adiciona, renomeia
 * código, marca extraviado, reativa e remove. Usado dentro do drawer de
 * edição de livro em LivrosPage.
 */
export function ExemplaresList({ livroId }: Props) {
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [renomeandoId, setRenomeandoId] = useState<number | null>(null);
  const [novoCodigo, setNovoCodigo] = useState('');

  const { data: exemplares, isLoading } = useQuery({
    queryKey: ['exemplares', livroId],
    queryFn: () => listarExemplares(livroId),
  });

  function invalidar() {
    queryClient.invalidateQueries({ queryKey: ['exemplares', livroId] });
    queryClient.invalidateQueries({ queryKey: ['livros'] });
  }

  const adicionar = useMutation({
    mutationFn: () => adicionarExemplar(livroId),
    onSuccess: () => {
      message.success('Exemplar adicionado');
      invalidar();
    },
    onError: (e) => message.error(mensagemDeErro(e)),
  });

  const renomear = useMutation({
    mutationFn: ({ id, codigo }: { id: number; codigo: string }) => renomearExemplar(id, codigo),
    onSuccess: () => {
      message.success('Código renomeado');
      setRenomeandoId(null);
      setNovoCodigo('');
      invalidar();
    },
    onError: (e) => message.error(mensagemDeErro(e)),
  });

  const extraviar = useMutation({
    mutationFn: (id: number) => marcarExtraviado(id),
    onSuccess: () => {
      message.success('Exemplar marcado como extraviado');
      invalidar();
    },
    onError: (e) => message.error(mensagemDeErro(e)),
  });

  const reativar = useMutation({
    mutationFn: (id: number) => reativarExemplar(id),
    onSuccess: () => {
      message.success('Exemplar reativado');
      invalidar();
    },
    onError: (e) => message.error(mensagemDeErro(e)),
  });

  const remover = useMutation({
    mutationFn: (id: number) => removerExemplar(id),
    onSuccess: () => {
      message.success('Exemplar removido');
      invalidar();
    },
    onError: (e) => message.error(mensagemDeErro(e)),
  });

  function comecarRenomear(e: ExemplarResponse) {
    setRenomeandoId(e.id);
    setNovoCodigo(e.codigo);
  }

  function confirmarRenomear() {
    if (renomeandoId && novoCodigo.trim()) {
      renomear.mutate({ id: renomeandoId, codigo: novoCodigo.trim() });
    }
  }

  if (isLoading) return <Skeleton active paragraph={{ rows: 3 }} />;

  const lista = exemplares ?? [];

  return (
    <div style={{ marginTop: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <Typography.Title level={5} style={{ margin: 0 }}>
          Exemplares ({lista.length})
        </Typography.Title>
        <Button
          size="small"
          type="dashed"
          icon={<PlusOutlined />}
          onClick={() => adicionar.mutate()}
          loading={adicionar.isPending}
        >
          Adicionar
        </Button>
      </div>

      {lista.length === 0 ? (
        <Empty description="Nenhum exemplar cadastrado" />
      ) : (
        <Space direction="vertical" size={8} style={{ width: '100%' }}>
          {lista.map((e) => (
            <div
              key={e.id}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                padding: 8,
                border: '1px solid #f0f0f0',
                borderRadius: 6,
                background: e.situacao === 'EXTRAVIADO' ? '#fff1f0' : '#fff',
              }}
            >
              {renomeandoId === e.id ? (
                <>
                  <Input
                    size="small"
                    value={novoCodigo}
                    onChange={(ev) => setNovoCodigo(ev.target.value)}
                    onPressEnter={confirmarRenomear}
                    style={{ flex: 1 }}
                    placeholder="Novo código (ex: 2024-A-042)"
                  />
                  <Button size="small" type="primary" onClick={confirmarRenomear} loading={renomear.isPending}>
                    Salvar
                  </Button>
                  <Button size="small" onClick={() => setRenomeandoId(null)}>
                    Cancelar
                  </Button>
                </>
              ) : (
                <>
                  <Typography.Text strong style={{ flex: 1, fontFamily: 'monospace' }}>
                    {e.codigo}
                  </Typography.Text>
                  <Tag color={COR_SITUACAO[e.situacao]}>{LABEL_SITUACAO[e.situacao]}</Tag>
                  <Button
                    size="small"
                    icon={<EditOutlined />}
                    onClick={() => comecarRenomear(e)}
                    title="Renomear código"
                  />
                  {e.situacao === 'DISPONIVEL' && (
                    <Popconfirm
                      title="Marcar como extraviado?"
                      description="O exemplar fica visível no histórico mas não poderá ser emprestado."
                      okText="Marcar"
                      okButtonProps={{ danger: true }}
                      onConfirm={() => extraviar.mutate(e.id)}
                    >
                      <Button size="small" danger icon={<WarningOutlined />} title="Marcar como extraviado" />
                    </Popconfirm>
                  )}
                  {e.situacao === 'EXTRAVIADO' && (
                    <Button
                      size="small"
                      icon={<ReloadOutlined />}
                      onClick={() => reativar.mutate(e.id)}
                      loading={reativar.isPending}
                      title="Reativar (apareceu de novo)"
                    >
                      Reativar
                    </Button>
                  )}
                  {e.situacao === 'DISPONIVEL' && (
                    <Popconfirm
                      title={`Remover ${e.codigo} permanentemente?`}
                      description="Só funciona se o exemplar nunca foi emprestado. Para preservar histórico, use 'extraviado'."
                      okText="Remover"
                      okButtonProps={{ danger: true }}
                      onConfirm={() => remover.mutate(e.id)}
                    >
                      <Button size="small" danger icon={<DeleteOutlined />} title="Remover exemplar" />
                    </Popconfirm>
                  )}
                </>
              )}
            </div>
          ))}
        </Space>
      )}
    </div>
  );
}
