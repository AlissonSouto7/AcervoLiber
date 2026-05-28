import { useEffect, useMemo, useState } from 'react';
import { DeleteOutlined, EditOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons';
import {
  Alert,
  App,
  Button,
  Card,
  Drawer,
  Form,
  Grid,
  Input,
  InputNumber,
  List,
  Popconfirm,
  Space,
  Tag,
  Typography,
  Upload,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  atualizarLivro,
  criarLivro,
  enviarCapa,
  listarLivros,
  removerCapaManual,
  removerLivro,
  type LivroPayload,
} from '../api/livros';
import { mensagemDeErro } from '../api/http';
import { CapaLivro } from '../components/CapaLivro';
import { CapaPreview } from '../components/CapaPreview';
import { ExemplaresList } from '../components/ExemplaresList';
import type { LivroResponse } from '../types/api';

const TAMANHO_PAGINA_PADRAO = 12;
const OPCOES_PAGINA = [12, 24, 48, 96];

/** Formatos e tamanho aceitos no upload de capa (espelha a validacao do backend). */
const TIPOS_IMAGEM = ['image/png', 'image/jpeg', 'image/webp'];
const TAMANHO_MAX_CAPA = 2 * 1024 * 1024;

/** Selo de disponibilidade do livro. */
function tagDisponibilidade(livro: LivroResponse) {
  const disp = livro.exemplaresDisponiveis;
  if (disp > 0) {
    return <Tag color="green">{disp} de {livro.exemplaresTotal} disponíveis</Tag>;
  }
  return <Tag color="red">Indisponível no momento</Tag>;
}

export default function LivrosPage() {
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.md;
  const { message } = App.useApp();
  const queryClient = useQueryClient();

  const [termo, setTermo] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(TAMANHO_PAGINA_PADRAO);
  const [drawerAberto, setDrawerAberto] = useState(false);
  const [editando, setEditando] = useState<LivroResponse | null>(null);
  /** Imagem de capa escolhida no formulario — enviada apos salvar o livro. */
  const [capaArquivo, setCapaArquivo] = useState<File | null>(null);

  // Instancia do formulario — usada para observar os campos em tempo real
  // e alimentar o preview da capa.
  const [form] = Form.useForm<LivroPayload>();
  const isbnDigitado = Form.useWatch('isbn', form);
  const tituloDigitado = Form.useWatch('titulo', form);
  const autorDigitado = Form.useWatch('autor', form);

  // URL local (blob) da imagem escolhida, para preview imediato sem upload.
  const capaArquivoPreview = useMemo(
    () => (capaArquivo ? URL.createObjectURL(capaArquivo) : null),
    [capaArquivo],
  );
  useEffect(() => {
    return () => {
      if (capaArquivoPreview) URL.revokeObjectURL(capaArquivoPreview);
    };
  }, [capaArquivoPreview]);

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['livros', termo, page, pageSize],
    queryFn: () => listarLivros({ termo, page, size: pageSize }),
  });

  const salvar = useMutation({
    mutationFn: async (valores: LivroPayload) => {
      const livro = editando
        ? await atualizarLivro(editando.id, valores)
        : await criarLivro(valores);
      // Se o admin escolheu uma imagem propria, envia depois de salvar o livro
      // (precisa do id). A capa manual substitui a automatica.
      if (capaArquivo) {
        await enviarCapa(livro.id, capaArquivo);
      }
      return livro;
    },
    onSuccess: () => {
      message.success(editando ? 'Livro atualizado' : 'Livro cadastrado');
      queryClient.invalidateQueries({ queryKey: ['livros'] });
      fecharDrawer();
    },
    onError: (erro) => message.error(mensagemDeErro(erro)),
  });

  const remover = useMutation({
    mutationFn: (id: number) => removerLivro(id),
    onSuccess: () => {
      message.success('Livro removido');
      queryClient.invalidateQueries({ queryKey: ['livros'] });
    },
    onError: (erro) => message.error(mensagemDeErro(erro)),
  });

  const restaurarCapaAutomatica = useMutation({
    mutationFn: (id: number) => removerCapaManual(id),
    onSuccess: () => {
      message.success('Capa automática restaurada');
      queryClient.invalidateQueries({ queryKey: ['livros'] });
      fecharDrawer();
    },
    onError: (erro) => message.error(mensagemDeErro(erro)),
  });

  function abrirNovo() {
    setEditando(null);
    setCapaArquivo(null);
    setDrawerAberto(true);
  }

  function abrirEdicao(livro: LivroResponse) {
    setEditando(livro);
    setCapaArquivo(null);
    setDrawerAberto(true);
  }

  function fecharDrawer() {
    setDrawerAberto(false);
    setEditando(null);
    setCapaArquivo(null);
  }

  /** Valida o arquivo escolhido e o guarda; nao faz upload aqui (so ao salvar). */
  function selecionarCapa(arquivo: File) {
    if (!TIPOS_IMAGEM.includes(arquivo.type)) {
      message.error('Formato inválido. Envie uma imagem JPG, PNG ou WEBP.');
      return Upload.LIST_IGNORE;
    }
    if (arquivo.size > TAMANHO_MAX_CAPA) {
      message.error('Imagem muito grande. O tamanho máximo é 2 MB.');
      return Upload.LIST_IGNORE;
    }
    setCapaArquivo(arquivo);
    return false; // impede o upload automatico do componente Upload
  }

  const valoresIniciais: Partial<LivroPayload> = editando
    ? {
        titulo: editando.titulo,
        autor: editando.autor,
        isbn: editando.isbn ?? undefined,
        ano: editando.ano ?? undefined,
        // exemplaresIniciais nao se aplica no edit — gestao de exemplares
        // acontece em endpoint separado (/livros/{id}/exemplares).
        sinopse: editando.sinopse ?? undefined,
      }
    : { exemplaresIniciais: 1 };

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
          Acervo
        </Typography.Title>
        <Space wrap style={{ flex: isMobile ? '1 1 100%' : undefined }}>
          <Input.Search
            placeholder="Buscar por título, autor ou ISBN"
            allowClear
            onSearch={(v) => {
              setTermo(v);
              setPage(0);
            }}
            style={{ width: isMobile ? '100%' : 280 }}
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={abrirNovo}>
            Novo livro
          </Button>
        </Space>
      </div>

      {isError && (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
          message="Não foi possível carregar a lista de livros"
          description={mensagemDeErro(error)}
          action={
            <Button size="small" onClick={() => refetch()}>
              Tentar novamente
            </Button>
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
            // AntD chama onChange com o novo pageSize quando o usuario muda
            // o seletor, mas mantem o pageSize antigo no proximo render se nao
            // atualizarmos o state — por isso setamos os dois aqui.
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
              cover={<CapaLivro titulo={livro.titulo} autor={livro.autor} capaUrl={livro.capaUrl} />}
              actions={[
                <EditOutlined key="editar" onClick={() => abrirEdicao(livro)} />,
                <Popconfirm
                  key="remover"
                  title="Remover este livro?"
                  description="Só é possível remover livros sem histórico de empréstimos."
                  okText="Remover"
                  cancelText="Cancelar"
                  okButtonProps={{ danger: true }}
                  onConfirm={() => remover.mutate(livro.id)}
                >
                  <DeleteOutlined />
                </Popconfirm>,
              ]}
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
              {tagDisponibilidade(livro)}
            </Card>
          </List.Item>
        )}
      />

      <Drawer
        title={editando ? 'Editar livro' : 'Novo livro'}
        open={drawerAberto}
        onClose={fecharDrawer}
        width={isMobile ? '100%' : 420}
      >
        {drawerAberto && (
          <Form<LivroPayload>
            key={editando?.id ?? 'novo'}
            form={form}
            layout="vertical"
            initialValues={valoresIniciais}
            onFinish={(valores) => salvar.mutate(valores)}
          >
            <Form.Item
              name="titulo"
              label="Título"
              rules={[{ required: true, message: 'Informe o título' }]}
            >
              <Input />
            </Form.Item>
            <Form.Item
              name="autor"
              label="Autor"
              rules={[{ required: true, message: 'Informe o autor' }]}
            >
              <Input />
            </Form.Item>
            <Form.Item name="isbn" label="ISBN">
              <Input placeholder="Opcional — usado para buscar a capa" />
            </Form.Item>

            <Form.Item label="Capa do livro">
              <div
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: 8,
                }}
              >
                {capaArquivoPreview ? (
                  <>
                    <div style={{ width: 130 }}>
                      <CapaLivro
                        titulo={tituloDigitado?.trim() || 'Título do livro'}
                        autor={autorDigitado?.trim() || undefined}
                        capaUrl={capaArquivoPreview}
                        altura={190}
                      />
                    </div>
                    <Typography.Text type="secondary" style={{ fontSize: 12, textAlign: 'center' }}>
                      Imagem escolhida — será salva junto com o livro
                    </Typography.Text>
                  </>
                ) : editando ? (
                  <>
                    <div style={{ width: 130 }}>
                      <CapaLivro
                        titulo={editando.titulo}
                        autor={editando.autor}
                        capaUrl={editando.capaUrl}
                        altura={190}
                      />
                    </div>
                    <Typography.Text type="secondary" style={{ fontSize: 12, textAlign: 'center' }}>
                      {editando.capaManual
                        ? 'Capa enviada manualmente'
                        : editando.capaUrl
                          ? 'Capa encontrada automaticamente'
                          : 'Sem capa — usando a capa gerada'}
                    </Typography.Text>
                  </>
                ) : (
                  <CapaPreview isbn={isbnDigitado} titulo={tituloDigitado} autor={autorDigitado} />
                )}

                <Space>
                  <Upload
                    accept="image/png,image/jpeg,image/webp"
                    showUploadList={false}
                    beforeUpload={selecionarCapa}
                  >
                    <Button icon={<UploadOutlined />} size="small">
                      {capaArquivo ? 'Trocar imagem' : 'Enviar imagem própria'}
                    </Button>
                  </Upload>
                  {capaArquivo && (
                    <Button size="small" type="text" danger onClick={() => setCapaArquivo(null)}>
                      Cancelar
                    </Button>
                  )}
                </Space>

                {!capaArquivo && editando?.capaManual && (
                  <Button
                    size="small"
                    type="link"
                    loading={restaurarCapaAutomatica.isPending}
                    onClick={() => restaurarCapaAutomatica.mutate(editando.id)}
                  >
                    Voltar para a capa automática
                  </Button>
                )}
              </div>
            </Form.Item>

            <Form.Item name="ano" label="Ano de publicação">
              <InputNumber style={{ width: '100%' }} min={1000} max={9999} placeholder="Opcional" />
            </Form.Item>
            {!editando && (
              <Form.Item
                name="exemplaresIniciais"
                label="Quantidade de exemplares iniciais"
                rules={[{ required: true, message: 'Informe a quantidade' }]}
                tooltip="Cada copia fisica ganha um codigo de tombamento proprio (LIB-XXXXX). Voce pode renomear depois pra casar com a etiqueta da escola."
              >
                <InputNumber style={{ width: '100%' }} min={1} max={100} />
              </Form.Item>
            )}
            <Form.Item
              name="sinopse"
              label="Sinopse (opcional)"
              tooltip="Se vazia, será preenchida automaticamente a partir do Google Books quando disponível."
              rules={[{ max: 2000, message: 'A sinopse deve ter no máximo 2000 caracteres' }]}
            >
              {/* showCount renderiza o contador absoluto e sobrepunha o texto do `extra`;
                  movemos a dica pro tooltip do label, mantendo o contador. */}
              <Input.TextArea rows={5} maxLength={2000} showCount placeholder="Breve resumo do livro" />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={salvar.isPending}>
              Salvar
            </Button>
          </Form>
        )}

        {/* Lista de exemplares — so aparece ao EDITAR (livro ja existe). No
            cadastro novo, os exemplares iniciais sao criados pelo campo
            'Exemplares iniciais' do form acima. */}
        {drawerAberto && editando && <ExemplaresList livroId={editando.id} />}
      </Drawer>
    </>
  );
}
