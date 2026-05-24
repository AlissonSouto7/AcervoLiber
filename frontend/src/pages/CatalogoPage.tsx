import { useState } from 'react';
import { Alert, App, Button, Card, Input, List, Tag, Typography } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { listarLivros } from '../api/livros';
import { reservarLivro, resumoReservas } from '../api/reservas';
import { mensagemDeErro } from '../api/http';
import { CapaLivro } from '../components/CapaLivro';

const TAMANHO_PAGINA = 12;

/** Catalogo de livros para o aluno — navegar e reservar. */
export default function CatalogoPage() {
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [termo, setTermo] = useState('');
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ['catalogo', termo, page],
    queryFn: () => listarLivros({ termo, page, size: TAMANHO_PAGINA }),
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
          pageSize: TAMANHO_PAGINA,
          total: data?.totalElements ?? 0,
          align: 'center',
          onChange: (p) => setPage(p - 1),
        }}
        renderItem={(livro) => (
          <List.Item>
            <Card
              hoverable
              size="small"
              styles={{ body: { padding: 12 } }}
              cover={<CapaLivro titulo={livro.titulo} autor={livro.autor} capaUrl={livro.capaUrl} />}
            >
              <Typography.Paragraph strong style={{ marginBottom: 2 }} ellipsis={{ rows: 2 }}>
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
              {livro.quantidadeDisponivel > 0 ? (
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
            </Card>
          </List.Item>
        )}
      />
    </>
  );
}
