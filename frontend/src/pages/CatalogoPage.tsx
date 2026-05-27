import { useState } from 'react';
import { Alert, App, Button, Card, Descriptions, Input, List, Modal, Tag, Typography } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { listarLivros } from '../api/livros';
import { reservarLivro, resumoReservas } from '../api/reservas';
import { mensagemDeErro } from '../api/http';
import { CapaLivro } from '../components/CapaLivro';
import type { LivroResponse } from '../types/api';

const TAMANHO_PAGINA_PADRAO = 12;
const OPCOES_PAGINA = [12, 24, 48, 96];

/** Catalogo de livros para o aluno — navegar e reservar. */
export default function CatalogoPage() {
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [termo, setTermo] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(TAMANHO_PAGINA_PADRAO);
  const [detalhe, setDetalhe] = useState<LivroResponse | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['catalogo', termo, page, pageSize],
    queryFn: () => listarLivros({ termo, page, size: pageSize }),
  });

  // Quantas vagas o aluno ainda tem (limite combinado de emprestimos + reservas).
  const { data: resumo } = useQuery({
    queryKey: ['resumo-reservas'],
    queryFn: resumoReservas,
  });
  const disponivel = resumo
    ? Math.max(0, resumo.limite - resumo.emprestimosAtivos - resumo.reservasPendentes)
    : 0;
  const semVagas = resumo ? disponivel <= 0 : false;

  const reservar = useMutation({
    mutationFn: (livroId: number) => reservarLivro(livroId),
    onSuccess: () => {
      message.success('Reserva feita! Retire o livro na biblioteca dentro do prazo.');
      queryClient.invalidateQueries({ queryKey: ['catalogo'] });
      queryClient.invalidateQueries({ queryKey: ['minhas-reservas'] });
      queryClient.invalidateQueries({ queryKey: ['resumo-reservas'] });
      setDetalhe(null);
    },
    onError: (erro) => message.error(mensagemDeErro(erro)),
  });

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
          Catálogo
        </Typography.Title>
        <Input.Search
          placeholder="Buscar por título, autor ou ISBN"
          allowClear
          onSearch={(v) => {
            setTermo(v);
            setPage(0);
          }}
          style={{ maxWidth: 280 }}
        />
      </div>

      {resumo && (
        <Alert
          style={{ marginBottom: 16 }}
          type={semVagas ? 'warning' : 'info'}
          showIcon
          message={
            semVagas
              ? `Você atingiu o limite de ${resumo.limite} livros entre empréstimos e reservas.`
              : `Você tem ${disponivel} de ${resumo.limite} vagas disponíveis ` +
                `(${resumo.emprestimosAtivos} empréstimo(s) e ${resumo.reservasPendentes} reserva(s)).`
          }
        />
      )}

      <List
        loading={isLoading}
        grid={{ gutter: 16, xs: 2, sm: 2, md: 3, lg: 4, xl: 5, xxl: 6 }}
        dataSource={data?.content ?? []}
        locale={{ emptyText: 'Nenhum livro encontrado' }}
        pagination={{
          current: page + 1,
          pageSize,
          total: data?.totalElements ?? 0,
          align: 'center',
          showSizeChanger: true,
          pageSizeOptions: OPCOES_PAGINA,
          showTotal: (total) => `${total} livro(s)`,
          onChange: (p, novoTamanho) => {
            if (novoTamanho !== pageSize) {
              setPageSize(novoTamanho);
              setPage(0);
            } else {
              setPage(p - 1);
            }
          },
        }}
        renderItem={(livro) => (
          <List.Item>
            <Card
              hoverable
              size="small"
              styles={{ body: { padding: 12 } }}
              cover={
                <div onClick={() => setDetalhe(livro)} style={{ cursor: 'pointer' }}>
                  <CapaLivro titulo={livro.titulo} autor={livro.autor} capaUrl={livro.capaUrl} />
                </div>
              }
            >
              <Typography.Paragraph
                strong
                style={{ marginBottom: 2, cursor: 'pointer' }}
                ellipsis={{ rows: 2 }}
                onClick={() => setDetalhe(livro)}
              >
                {livro.titulo}
              </Typography.Paragraph>
              <Typography.Paragraph
                type="secondary"
                style={{ marginBottom: 8, fontSize: 12 }}
                ellipsis={{ rows: 1 }}
              >
                {livro.autor}
                {livro.ano ? ` · ${livro.ano}` : ''}
              </Typography.Paragraph>
              {livro.exemplaresDisponiveis > 0 ? (
                <Button
                  type="primary"
                  block
                  size="small"
                  disabled={semVagas}
                  loading={reservar.isPending && reservar.variables === livro.id}
                  onClick={() => reservar.mutate(livro.id)}
                >
                  Reservar
                </Button>
              ) : (
                <Tag color="red" style={{ width: '100%', textAlign: 'center', marginInlineEnd: 0 }}>
                  Indisponível
                </Tag>
              )}
              <Button
                type="link"
                size="small"
                block
                style={{ marginTop: 4 }}
                onClick={() => setDetalhe(livro)}
              >
                Ver detalhes
              </Button>
            </Card>
          </List.Item>
        )}
      />

      <Modal
        open={!!detalhe}
        onCancel={() => setDetalhe(null)}
        title={detalhe?.titulo}
        width={720}
        footer={
          detalhe ? (
            detalhe.exemplaresDisponiveis > 0 ? (
              <Button
                type="primary"
                disabled={semVagas}
                loading={reservar.isPending && reservar.variables === detalhe.id}
                onClick={() => reservar.mutate(detalhe.id)}
              >
                Reservar
              </Button>
            ) : (
              <Tag color="red">Indisponível</Tag>
            )
          ) : null
        }
      >
        {detalhe && (
          <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap' }}>
            <div style={{ width: 180, flexShrink: 0 }}>
              <CapaLivro
                titulo={detalhe.titulo}
                autor={detalhe.autor}
                capaUrl={detalhe.capaUrl}
                altura={260}
              />
            </div>
            <div style={{ flex: '1 1 320px' }}>
              <Descriptions column={1} size="small" colon={false}>
                <Descriptions.Item label="Autor">{detalhe.autor}</Descriptions.Item>
                {detalhe.ano && <Descriptions.Item label="Ano">{detalhe.ano}</Descriptions.Item>}
                {detalhe.isbn && <Descriptions.Item label="ISBN">{detalhe.isbn}</Descriptions.Item>}
                <Descriptions.Item label="Disponíveis">
                  {detalhe.exemplaresDisponiveis} de {detalhe.exemplaresTotal}
                </Descriptions.Item>
              </Descriptions>
              <Typography.Title level={5} style={{ marginTop: 16, marginBottom: 8 }}>
                Sinopse
              </Typography.Title>
              {detalhe.sinopse ? (
                <Typography.Paragraph style={{ whiteSpace: 'pre-wrap' }}>
                  {detalhe.sinopse}
                </Typography.Paragraph>
              ) : (
                <Typography.Text type="secondary">Sinopse ainda não disponível.</Typography.Text>
              )}
            </div>
          </div>
        )}
      </Modal>
    </>
  );
}
