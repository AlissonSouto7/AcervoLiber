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

/** Numidia — gatinha branca com coroa, bochechas rosadas, olhos grandes pretos. */
function Numidia() {
  return (
    <svg viewBox="0 0 64 64" width="64" height="64" xmlns="http://www.w3.org/2000/svg">
      {/* Sombra/circulo de fundo cinza */}
      <circle cx="32" cy="34" r="28" fill="#3a3a45" />

      {/* Orelhas brancas com interior rosa */}
      <path d="M 17 18 L 19 28 L 26 22 Z" fill="#ffffff" stroke="#d0d0d0" strokeWidth="0.5" />
      <path d="M 19 22 L 20 26 L 23 23 Z" fill="#f8c8d8" />
      <path d="M 47 18 L 45 28 L 38 22 Z" fill="#ffffff" stroke="#d0d0d0" strokeWidth="0.5" />
      <path d="M 45 22 L 44 26 L 41 23 Z" fill="#f8c8d8" />

      {/* Coroa amarela */}
      <path
        d="M 25 14 L 27 18 L 30 13 L 32 17 L 34 13 L 37 18 L 39 14 L 38 21 L 26 21 Z"
        fill="#ffd24a"
        stroke="#c99617"
        strokeWidth="0.6"
        strokeLinejoin="round"
      />
      <circle cx="27" cy="14" r="1" fill="#ff5575" />
      <circle cx="32" cy="13" r="1" fill="#54b3ff" />
      <circle cx="37" cy="14" r="1" fill="#ff5575" />

      {/* Cabeça branca redonda */}
      <ellipse cx="32" cy="34" rx="20" ry="18" fill="#ffffff" stroke="#e0e0e0" strokeWidth="0.5" />

      {/* Bochechas rosadas */}
      <ellipse cx="20" cy="38" rx="4" ry="2.5" fill="#fdb8c8" opacity="0.8" />
      <ellipse cx="44" cy="38" rx="4" ry="2.5" fill="#fdb8c8" opacity="0.8" />

      {/* Olhos grandes pretos com brilho */}
      <ellipse cx="25" cy="33" rx="3" ry="4.5" fill="#1a1a1a" />
      <ellipse cx="39" cy="33" rx="3" ry="4.5" fill="#1a1a1a" />
      <ellipse cx="26" cy="31" rx="1" ry="1.5" fill="#ffffff" />
      <ellipse cx="40" cy="31" rx="1" ry="1.5" fill="#ffffff" />
      <ellipse cx="24" cy="35" rx="0.5" ry="0.8" fill="#ffffff" opacity="0.7" />
      <ellipse cx="38" cy="35" rx="0.5" ry="0.8" fill="#ffffff" opacity="0.7" />

      {/* Nariz triangulo rosa */}
      <path d="M 31 39 L 33 39 L 32 41 Z" fill="#ff8aa3" />

      {/* Boca pequena - dois arquinhos */}
      <path d="M 32 41 Q 30 43, 28 42" stroke="#1a1a1a" strokeWidth="0.8" fill="none" />
      <path d="M 32 41 Q 34 43, 36 42" stroke="#1a1a1a" strokeWidth="0.8" fill="none" />

      {/* Bigodes finos */}
      <line x1="13" y1="37" x2="18" y2="38" stroke="#888" strokeWidth="0.4" />
      <line x1="13" y1="40" x2="18" y2="40" stroke="#888" strokeWidth="0.4" />
      <line x1="46" y1="38" x2="51" y2="37" stroke="#888" strokeWidth="0.4" />
      <line x1="46" y1="40" x2="51" y2="40" stroke="#888" strokeWidth="0.4" />

      {/* Patinhas em frente */}
      <ellipse cx="27" cy="56" rx="4" ry="3" fill="#ffffff" stroke="#e0e0e0" strokeWidth="0.5" />
      <ellipse cx="37" cy="56" rx="4" ry="3" fill="#ffffff" stroke="#e0e0e0" strokeWidth="0.5" />
    </svg>
  );
}
