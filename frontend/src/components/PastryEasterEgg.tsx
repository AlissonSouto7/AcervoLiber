import { useState } from 'react';

/**
 * Easter egg no canto do login: croissant frances que vira a gatinha Numidia
 * ao passar o mouse (desktop) ou tocar (mobile). Homenagem fofa, totalmente
 * decorativa, nao afeta a usabilidade da tela de login.
 */
export function PastryEasterEgg() {
  const [revelado, setRevelado] = useState(false);

  return (
    <div
      role="img"
      aria-label={revelado ? 'Numidia, a gatinha' : 'Croissant francês'}
      onMouseEnter={() => setRevelado(true)}
      onMouseLeave={() => setRevelado(false)}
      onClick={() => setRevelado((v) => !v)}
      style={{
        width: 64,
        height: 64,
        cursor: 'pointer',
        userSelect: 'none',
        transition: 'transform 0.3s ease',
        transform: revelado ? 'scale(1.1)' : 'scale(1)',
        margin: '0 auto 12px',
      }}
    >
      {revelado ? <Numidia /> : <Croissant />}
    </div>
  );
}

/** Croissant frances classico — curvado, dourado, com sombreado em camadas. */
function Croissant() {
  return (
    <svg viewBox="0 0 64 64" width="64" height="64" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="croissantGrad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#f5d07a" />
          <stop offset="50%" stopColor="#d49a3a" />
          <stop offset="100%" stopColor="#a8722a" />
        </linearGradient>
      </defs>
      {/* Corpo principal: curva tipo lua crescente */}
      <path
        d="M 8 36 Q 8 14, 32 12 Q 56 14, 56 36 Q 50 30, 32 30 Q 14 30, 8 36 Z"
        fill="url(#croissantGrad)"
        stroke="#8a5a1f"
        strokeWidth="1.2"
        strokeLinejoin="round"
      />
      {/* Vincos/camadas da massa folhada */}
      <path d="M 16 22 Q 18 28, 16 33" stroke="#8a5a1f" strokeWidth="1" fill="none" opacity="0.6" />
      <path d="M 24 17 Q 25 26, 23 32" stroke="#8a5a1f" strokeWidth="1" fill="none" opacity="0.6" />
      <path d="M 32 14 Q 32 24, 32 31" stroke="#8a5a1f" strokeWidth="1" fill="none" opacity="0.6" />
      <path d="M 40 17 Q 39 26, 41 32" stroke="#8a5a1f" strokeWidth="1" fill="none" opacity="0.6" />
      <path d="M 48 22 Q 46 28, 48 33" stroke="#8a5a1f" strokeWidth="1" fill="none" opacity="0.6" />
      {/* Brilho superior */}
      <path
        d="M 20 18 Q 32 14, 44 18"
        stroke="#fff3d4"
        strokeWidth="1.5"
        fill="none"
        opacity="0.5"
      />
    </svg>
  );
}

/** Numidia — gatinha branca com coroa, refinada a partir de referencia visual. */
function Numidia() {
  return (
    <svg viewBox="0 0 64 64" width="64" height="64" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <radialGradient id="bgNumidia" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="#4a4a55" />
          <stop offset="100%" stopColor="#2a2a32" />
        </radialGradient>
        <linearGradient id="coroa" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="#ffe27a" />
          <stop offset="100%" stopColor="#d49a1a" />
        </linearGradient>
      </defs>

      {/* Avatar circulo de fundo */}
      <circle cx="32" cy="32" r="30" fill="url(#bgNumidia)" />

      {/* Coroa pequena e elegante entre as orelhas */}
      <path
        d="M 27 13 L 29 17 L 32 12 L 35 17 L 37 13 L 36.5 19 L 27.5 19 Z"
        fill="url(#coroa)"
        stroke="#a87410"
        strokeWidth="0.5"
        strokeLinejoin="round"
      />
      <circle cx="32" cy="13.5" r="0.9" fill="#ff6b8a" />

      {/* Orelhas altas brancas com interior rosado */}
      <path d="M 17 22 L 21 13 L 27 21 Z" fill="#ffffff" stroke="#c8c8d0" strokeWidth="0.5" strokeLinejoin="round" />
      <path d="M 20 19 L 22 17 L 24 20 Z" fill="#fac5d5" />
      <path d="M 47 22 L 43 13 L 37 21 Z" fill="#ffffff" stroke="#c8c8d0" strokeWidth="0.5" strokeLinejoin="round" />
      <path d="M 44 19 L 42 17 L 40 20 Z" fill="#fac5d5" />

      {/* Cabeça branca redondinha */}
      <ellipse cx="32" cy="33" rx="17" ry="15" fill="#ffffff" stroke="#d8d8e0" strokeWidth="0.4" />

      {/* Bochechas rosadas discretas */}
      <ellipse cx="22" cy="37" rx="3" ry="1.8" fill="#fdb8c8" opacity="0.7" />
      <ellipse cx="42" cy="37" rx="3" ry="1.8" fill="#fdb8c8" opacity="0.7" />

      {/* Olhos amendoados (mais delicados que circulos grandes) */}
      <ellipse cx="26" cy="33" rx="2.2" ry="3.2" fill="#1a1a22" />
      <ellipse cx="38" cy="33" rx="2.2" ry="3.2" fill="#1a1a22" />
      {/* Brilhos primarios (grandes) */}
      <ellipse cx="26.8" cy="31.5" rx="0.9" ry="1.2" fill="#ffffff" />
      <ellipse cx="38.8" cy="31.5" rx="0.9" ry="1.2" fill="#ffffff" />
      {/* Brilhos secundarios (pequenos) */}
      <circle cx="25" cy="34.5" r="0.4" fill="#ffffff" opacity="0.6" />
      <circle cx="37" cy="34.5" r="0.4" fill="#ffffff" opacity="0.6" />

      {/* Nariz triangulo rosa, bem pequeno */}
      <path d="M 31 39 L 33 39 L 32 40.5 Z" fill="#ff8aa3" />

      {/* Boca em "V" delicado (sorriso fechado) */}
      <path d="M 30 41 L 32 42.5 L 34 41" stroke="#1a1a22" strokeWidth="0.8" fill="none" strokeLinecap="round" strokeLinejoin="round" />

      {/* Patinhas curtas em frente do corpinho */}
      <ellipse cx="26" cy="52" rx="3.5" ry="2.5" fill="#ffffff" stroke="#d8d8e0" strokeWidth="0.4" />
      <ellipse cx="38" cy="52" rx="3.5" ry="2.5" fill="#ffffff" stroke="#d8d8e0" strokeWidth="0.4" />
      {/* Sombra entre as patinhas pra dar profundidade */}
      <ellipse cx="32" cy="54" rx="3" ry="0.8" fill="#000" opacity="0.15" />
    </svg>
  );
}
