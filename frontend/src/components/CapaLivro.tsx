import { useEffect, useState } from 'react';
import { BookOutlined } from '@ant-design/icons';
import { resolverUrlCapa } from '../config';

/** Pares de cores para o gradiente da capa gerada. */
const PALETA: [string, string][] = [
  ['#1d3557', '#457b9d'],
  ['#2d6a4f', '#52b788'],
  ['#6d597a', '#b56576'],
  ['#7c3626', '#bc6c25'],
  ['#3d348b', '#7678ed'],
  ['#264653', '#2a9d8f'],
  ['#583101', '#a47148'],
  ['#3a0ca3', '#4361ee'],
];

/** Escolhe um par de cores de forma estavel a partir do titulo. */
function coresDoTitulo(titulo: string): [string, string] {
  let hash = 0;
  for (let i = 0; i < titulo.length; i++) {
    hash = titulo.charCodeAt(i) + ((hash << 5) - hash);
  }
  return PALETA[Math.abs(hash) % PALETA.length];
}

interface CapaLivroProps {
  titulo: string;
  autor?: string;
  /** URL da capa (externa, do nosso backend, ou blob de preview). */
  capaUrl?: string | null;
  altura?: number;
}

/**
 * Capa visual de um livro — componente puramente apresentacional.
 *
 * Sempre desenha uma "capa gerada" (gradiente + titulo) como base; se houver
 * `capaUrl`, a imagem real e exibida por cima, com **fade-in** suave quando
 * termina de carregar (a capa gerada faz as vezes de placeholder enquanto isso).
 * Se a imagem falhar, a capa gerada permanece — nunca quebra.
 */
export function CapaLivro({ titulo, autor, capaUrl, altura = 210 }: CapaLivroProps) {
  const [c1, c2] = coresDoTitulo(titulo);
  const [imagemFalhou, setImagemFalhou] = useState(false);
  const [imagemCarregada, setImagemCarregada] = useState(false);

  // Ao trocar de capa (ex.: digitando o ISBN no preview), reinicia os estados.
  useEffect(() => {
    setImagemFalhou(false);
    setImagemCarregada(false);
  }, [capaUrl]);

  return (
    <div
      style={{
        position: 'relative',
        height: altura,
        background: `linear-gradient(135deg, ${c1}, ${c2})`,
        color: '#fff',
        overflow: 'hidden',
      }}
    >
      {/* Capa gerada — base sempre presente (placeholder enquanto a imagem carrega) */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          padding: 16,
        }}
      >
        <BookOutlined style={{ fontSize: 22, opacity: 0.7 }} />
        <div>
          <div style={{ fontSize: 15, fontWeight: 700, lineHeight: 1.3 }}>{titulo}</div>
          {autor && (
            <div style={{ fontSize: 12, opacity: 0.85, marginTop: 4 }}>{autor}</div>
          )}
        </div>
      </div>

      {/* Capa real — sobreposta, com fade-in ao carregar */}
      {capaUrl && !imagemFalhou && (
        <img
          src={resolverUrlCapa(capaUrl)}
          alt={`Capa de ${titulo}`}
          loading="lazy"
          // no-referrer: nao envia o cabecalho Referer ao host externo da capa
          // (Google Books / Open Library). Sem isso, cada render vaza a URL da
          // app do usuario para terceiros — fingerprinting/correlacao
          // comportamental, indesejavel para acervo de menores (LGPD).
          referrerPolicy="no-referrer"
          onLoad={() => setImagemCarregada(true)}
          onError={() => setImagemFalhou(true)}
          style={{
            position: 'absolute',
            inset: 0,
            width: '100%',
            height: '100%',
            objectFit: 'cover',
            opacity: imagemCarregada ? 1 : 0,
            transition: 'opacity 0.35s ease',
          }}
        />
      )}
    </div>
  );
}
