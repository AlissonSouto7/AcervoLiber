/**
 * Avatar da Numidia no topo do login — homenagem fofa, totalmente decorativa.
 * Desenhada em SVG inline (sem dependencia externa, sempre carrega).
 */
export function PastryEasterEgg() {
  return (
    <div
      role="img"
      aria-label="Numidia, a gatinha"
      title="Numidia 🐱"
      style={{
        width: 72,
        height: 72,
        margin: '0 auto 12px',
        userSelect: 'none',
      }}
    >
      <Numidia />
    </div>
  );
}

/** Numidia — gatinha branca com coroa, refinada com base na referencia. */
function Numidia() {
  return (
    <svg viewBox="0 0 64 64" width="72" height="72" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <radialGradient id="bgNumidia" cx="35%" cy="30%" r="80%">
          <stop offset="0%" stopColor="#5a5a68" />
          <stop offset="100%" stopColor="#2a2a32" />
        </radialGradient>
      </defs>

      {/* Avatar circulo escuro de fundo */}
      <circle cx="32" cy="32" r="30" fill="url(#bgNumidia)" />

      {/* Corpinho/sombra no fundo do circulo (a gatinha esta sentada) */}
      <ellipse cx="32" cy="58" rx="14" ry="6" fill="#ffffff" opacity="0.7" />

      {/* Orelhas brancas arredondadas, baixinhas, com interior rosa clarinho */}
      <path d="M 18 22 Q 17 14, 22 13 Q 26 16, 26 22 Z" fill="#ffffff" stroke="#d8d8e0" strokeWidth="0.4" />
      <path d="M 20 20 Q 20 17, 23 17 Q 24 19, 24 21 Z" fill="#fad5dd" />
      <path d="M 46 22 Q 47 14, 42 13 Q 38 16, 38 22 Z" fill="#ffffff" stroke="#d8d8e0" strokeWidth="0.4" />
      <path d="M 44 20 Q 44 17, 41 17 Q 40 19, 40 21 Z" fill="#fad5dd" />

      {/* Coroa pequena dourada de 3 pontas finas */}
      <path
        d="M 27 12 L 28.5 16 L 32 11 L 35.5 16 L 37 12 L 36 17 L 28 17 Z"
        fill="#ffd24a"
        stroke="#c0941a"
        strokeWidth="0.4"
        strokeLinejoin="round"
      />
      <circle cx="32" cy="12.5" r="0.7" fill="#ff7b9d" />

      {/* Cabeça branca redonda */}
      <ellipse cx="32" cy="33" rx="16" ry="14.5" fill="#ffffff" stroke="#dadae2" strokeWidth="0.4" />

      {/* Bochechas rosadas pequenas e discretas */}
      <ellipse cx="22.5" cy="37" rx="2.5" ry="1.4" fill="#fdc3d0" opacity="0.85" />
      <ellipse cx="41.5" cy="37" rx="2.5" ry="1.4" fill="#fdc3d0" opacity="0.85" />

      {/* Olhos grandes redondos pretos com brilho */}
      <ellipse cx="26" cy="33" rx="2.6" ry="3" fill="#1a1a22" />
      <ellipse cx="38" cy="33" rx="2.6" ry="3" fill="#1a1a22" />
      {/* Brilho grande no topo de cada olho */}
      <ellipse cx="26.8" cy="31.6" rx="1" ry="1.3" fill="#ffffff" />
      <ellipse cx="38.8" cy="31.6" rx="1" ry="1.3" fill="#ffffff" />
      {/* Brilho secundario pequeno */}
      <circle cx="25.2" cy="34.3" r="0.4" fill="#ffffff" opacity="0.7" />
      <circle cx="37.2" cy="34.3" r="0.4" fill="#ffffff" opacity="0.7" />

      {/* Nariz triangulo rosa minusculo */}
      <path d="M 31.2 39 L 32.8 39 L 32 40.3 Z" fill="#ff8aa3" />

      {/* Boca sorridente (dois arquinhos pequenos formando um U invertido) */}
      <path
        d="M 32 40.5 Q 30 42.5, 28.5 41.8"
        stroke="#1a1a22"
        strokeWidth="0.85"
        fill="none"
        strokeLinecap="round"
      />
      <path
        d="M 32 40.5 Q 34 42.5, 35.5 41.8"
        stroke="#1a1a22"
        strokeWidth="0.85"
        fill="none"
        strokeLinecap="round"
      />

      {/* Patinhas curtas e brancas em frente do corpinho */}
      <ellipse cx="27" cy="55" rx="3.2" ry="2.5" fill="#ffffff" stroke="#dadae2" strokeWidth="0.4" />
      <ellipse cx="37" cy="55" rx="3.2" ry="2.5" fill="#ffffff" stroke="#dadae2" strokeWidth="0.4" />
    </svg>
  );
}
