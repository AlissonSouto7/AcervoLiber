import { useEffect, useState } from 'react';
import { Typography } from 'antd';
import { LoadingOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { buscarCapa } from '../api/capas';
import { CapaLivro } from './CapaLivro';

interface CapaPreviewProps {
  isbn?: string | null;
  titulo?: string;
  autor?: string;
}

/** Debounce simples: so devolve o valor depois de `ms` sem mudancas. */
function useDebounced<T>(valor: T, ms: number): T {
  const [debounced, setDebounced] = useState(valor);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(valor), ms);
    return () => clearTimeout(t);
  }, [valor, ms]);
  return debounced;
}

/** Quantidade de caracteres validos (digitos/X) — ISBN tem 10 ou 13. */
function digitosIsbn(isbn: string): number {
  return isbn.replace(/[^0-9Xx]/g, '').length;
}

/**
 * Pre-visualizacao da capa no formulario de livro: conforme o ISBN OU o
 * titulo/autor sao digitados, busca a capa no backend e mostra. Antes disso,
 * exibe a capa gerada (gradiente) com o titulo digitado.
 */
export function CapaPreview({ isbn, titulo, autor }: CapaPreviewProps) {
  const isbnDeb = useDebounced(isbn ?? '', 600);
  const tituloDeb = useDebounced(titulo ?? '', 600);
  const autorDeb = useDebounced(autor ?? '', 600);

  const isbnCompleto = digitosIsbn(isbnDeb) >= 10;
  const tituloUtilizavel = tituloDeb.trim().length >= 3;
  const podeBuscar = isbnCompleto || tituloUtilizavel;

  const { data: capaUrl, isFetching } = useQuery({
    queryKey: [
      'capa-preview',
      isbnDeb.replace(/[^0-9Xx]/g, ''),
      tituloDeb.trim().toLowerCase(),
      autorDeb.trim().toLowerCase(),
    ],
    queryFn: () => buscarCapa({ isbn: isbnDeb, titulo: tituloDeb, autor: autorDeb }),
    enabled: podeBuscar,
    staleTime: Infinity,
    gcTime: Infinity,
    retry: false,
  });

  let legenda: React.ReactNode;
  if (!podeBuscar) {
    legenda = 'Preencha o titulo ou o ISBN para buscar a capa';
  } else if (isFetching) {
    legenda = (
      <>
        <LoadingOutlined /> Buscando capa...
      </>
    );
  } else if (capaUrl) {
    legenda = 'Capa encontrada automaticamente';
  } else {
    legenda = 'Sem capa automatica — sera usada a capa gerada';
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
      <div style={{ width: 130 }}>
        <CapaLivro
          titulo={titulo?.trim() || 'Titulo do livro'}
          autor={autor?.trim() || undefined}
          capaUrl={podeBuscar ? capaUrl : null}
          altura={190}
        />
      </div>
      <Typography.Text type="secondary" style={{ fontSize: 12, textAlign: 'center' }}>
        {legenda}
      </Typography.Text>
    </div>
  );
}
