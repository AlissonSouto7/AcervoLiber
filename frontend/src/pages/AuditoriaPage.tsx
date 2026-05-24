import { useState } from 'react';
import { Card, Grid, List, Select, Space, Table, Tag, Typography, type TableProps } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { listarAuditoria } from '../api/auditoria';
import type { AuditLogResponse, EventoAuditoria } from '../types/api';
import { formatarDataHora } from '../utils';

const TAMANHO_PAGINA = 15;

const EVENTO_INFO: Record<EventoAuditoria, { cor: string; texto: string }> = {
  LOGIN_SUCESSO: { cor: 'green', texto: 'Login OK' },
  LOGIN_FALHA: { cor: 'red', texto: 'Login falhou' },
  LOGIN_BLOQUEADO: { cor: 'volcano', texto: 'Login bloqueado' },
  LOGOUT: { cor: 'default', texto: 'Logout' },
  TROCA_SENHA: { cor: 'blue', texto: 'Troca de senha' },
  PERFIL_ATUALIZADO: { cor: 'geekblue', texto: 'Perfil atualizado' },
  USUARIO_CRIADO: { cor: 'green', texto: 'Usuário criado' },
  USUARIO_ATIVADO: { cor: 'cyan', texto: 'Usuário ativado' },
  USUARIO_DESATIVADO: { cor: 'orange', texto: 'Usuário desativado' },
  REFRESH_REUSO: { cor: 'magenta', texto: 'Reuso de refresh (suspeita de roubo)' },
  ACESSO_NEGADO: { cor: 'red', texto: 'Acesso negado' },
  EMPRESTIMO_REGISTRADO: { cor: 'blue', texto: 'Empréstimo registrado' },
  EMPRESTIMO_DEVOLVIDO: { cor: 'cyan', texto: 'Empréstimo devolvido' },
  ESTOQUE_DIVERGENCIA: { cor: 'volcano', texto: 'Estoque divergente' },
};

function tagEvento(evento: EventoAuditoria) {
  // Fallback defensivo: backend pode adicionar evento novo antes do deploy do
  // frontend casar; mostrar o nome cru evita quebrar a pagina inteira.
  const info = EVENTO_INFO[evento] ?? { cor: 'default', texto: evento };
  return <Tag color={info.cor}>{info.texto}</Tag>;
}

// LOGIN_SUCESSO nao e mais registrado (decisao de produto). Excluido das opcoes
// de filtro para nao confundir o admin com "filtre por isso e nao vem nada".
// Mantido no EVENTO_INFO acima para nao quebrar registros legados que ainda
// possam existir no banco.
const OPCOES_EVENTO = (Object.keys(EVENTO_INFO) as EventoAuditoria[])
  .filter((chave) => chave !== 'LOGIN_SUCESSO')
  .map((chave) => ({ value: chave, label: EVENTO_INFO[chave].texto }));

export default function AuditoriaPage() {
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.md;
  const [evento, setEvento] = useState<EventoAuditoria | undefined>(undefined);
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ['auditoria', evento, page],
    queryFn: () => listarAuditoria({ evento, page, size: TAMANHO_PAGINA }),
  });

  const colunas: TableProps<AuditLogResponse>['columns'] = [
    { title: 'Quando', key: 'quando', width: 170, render: (_, e) => formatarDataHora(e.ocorridoEm) },
    { title: 'Evento', key: 'evento', width: 160, render: (_, e) => tagEvento(e.evento) },
    { title: 'Alvo', dataIndex: 'usuarioEmail', render: (v) => v ?? '—' },
    { title: 'Executado por', dataIndex: 'atorEmail', render: (v) => v ?? '—' },
    { title: 'IP', dataIndex: 'ip', width: 130, render: (v) => v ?? '—' },
    { title: 'Detalhe', dataIndex: 'detalhe', render: (v) => v ?? '—' },
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
          Auditoria
        </Typography.Title>
        <Select<EventoAuditoria>
          allowClear
          placeholder="Filtrar por evento"
          style={{ width: isMobile ? '100%' : 240 }}
          options={OPCOES_EVENTO}
          value={evento}
          onChange={(v) => {
            setEvento(v);
            setPage(0);
          }}
        />
      </div>

      {isMobile ? (
        <List
          loading={isLoading}
          dataSource={data?.content ?? []}
          locale={{ emptyText: 'Nenhum evento registrado' }}
          pagination={paginacao}
          renderItem={(e) => (
            <Card size="small" style={{ marginBottom: 12 }}>
              <Space wrap style={{ justifyContent: 'space-between', width: '100%' }}>
                {tagEvento(e.evento)}
                <Typography.Text type="secondary">{formatarDataHora(e.ocorridoEm)}</Typography.Text>
              </Space>
              <div style={{ marginTop: 6 }}>
                <Typography.Text>{e.usuarioEmail ?? '—'}</Typography.Text>
              </div>
              <div>
                <Typography.Text type="secondary">
                  IP {e.ip ?? '—'}
                  {e.detalhe ? ` · ${e.detalhe}` : ''}
                </Typography.Text>
              </div>
            </Card>
          )}
        />
      ) : (
        <Table<AuditLogResponse>
          rowKey="id"
          loading={isLoading}
          columns={colunas}
          dataSource={data?.content ?? []}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: 'Nenhum evento registrado' }}
          pagination={paginacao}
        />
      )}
    </>
  );
}
