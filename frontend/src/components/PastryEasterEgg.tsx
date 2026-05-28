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

/**
 * Numidia — gatinha branca com coroa, busto fundido no fundo do avatar e
 * bracinhos estendidos pros lados (estilo "ola"). Refinada com base em
 * referencias visuais fornecidas pelo usuario.
 */
function Numidia() {
  return (
    <svg viewBox="0 0 80 80" width="72" height="72" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <radialGradient id="bgNumidia" cx="35%" cy="30%" r="80%">
          <stop offset="0%" stopColor="#5a5a68" />
          <stop offset="100%" stopColor="#2a2a32" />
        </radialGradient>
        <clipPath id="clipCirculo">
          <circle cx="40" cy="40" r="38" />
        </clipPath>
      </defs>

      {/* Avatar circulo escuro de fundo */}
      <circle cx="40" cy="40" r="38" fill="url(#bgNumidia)" />

      <g clipPath="url(#clipCirculo)">
        {/* ============ BARRIGUINHA (sem pernas, some no fundo do circulo) ============ */}
        {/* Tronco em forma de sino: estreito perto do pescoco, alarga e desce ate o
            limite do circulo. O clipPath corta automaticamente o que passa. */}
        <path
          d="M 32 50
             Q 30 55, 30 62
             Q 30 78, 40 80
             Q 50 78, 50 62
             Q 50 55, 48 50 Z"
          fill="#ffffff"
          stroke="#dadae2"
          strokeWidth="0.5"
          strokeLinejoin="round"
        />

        {/* ============ BRACINHOS ABERTOS PROS LADOS (estilo "ola") ============ */}
        {/* Brace esquerdo: traco grosso curvo do peito ate fora da cintura */}
        <path
          d="M 32 56 Q 25 60, 22 67"
          stroke="#ffffff"
          strokeWidth="6"
          fill="none"
          strokeLinecap="round"
        />
        <path
          d="M 32 56 Q 25 60, 22 67"
          stroke="#dadae2"
          strokeWidth="0.5"
          fill="none"
          strokeLinecap="round"
        />
        {/* Brace direito (simetrico) */}
        <path
          d="M 48 56 Q 55 60, 58 67"
          stroke="#ffffff"
          strokeWidth="6"
          fill="none"
          strokeLinecap="round"
        />
        <path
          d="M 48 56 Q 55 60, 58 67"
          stroke="#dadae2"
          strokeWidth="0.5"
          fill="none"
          strokeLinecap="round"
        />
        {/* Maozinhas redondas na ponta dos bracos */}
        <circle cx="22" cy="67" r="3.2" fill="#ffffff" stroke="#dadae2" strokeWidth="0.5" />
        <circle cx="58" cy="67" r="3.2" fill="#ffffff" stroke="#dadae2" strokeWidth="0.5" />

        {/* ============ CABEÇA (a parte que voce aprovou) ============ */}
        {/* Orelhas brancas arredondadas com interior rosa */}
        <path d="M 24 26 Q 23 15, 29 14 Q 33 18, 33 26 Z" fill="#ffffff" stroke="#dadae2" strokeWidth="0.5" strokeLinejoin="round" />
        <path d="M 26 23 Q 26 19, 30 19 Q 31 21, 31 24 Z" fill="#fad5dd" />
        <path d="M 56 26 Q 57 15, 51 14 Q 47 18, 47 26 Z" fill="#ffffff" stroke="#dadae2" strokeWidth="0.5" strokeLinejoin="round" />
        <path d="M 54 23 Q 54 19, 50 19 Q 49 21, 49 24 Z" fill="#fad5dd" />

        {/* Coroa pequena dourada */}
        <path
          d="M 33 13 L 35 17 L 40 12 L 45 17 L 47 13 L 46 19 L 34 19 Z"
          fill="#ffd24a"
          stroke="#c0941a"
          strokeWidth="0.5"
          strokeLinejoin="round"
        />
        <circle cx="40" cy="13.2" r="0.9" fill="#ff7b9d" />

        {/* Cabeça branca redonda dominante */}
        <circle cx="40" cy="34" r="17" fill="#ffffff" stroke="#dadae2" strokeWidth="0.5" />

        {/* Bochechas rosadas pequenas */}
        <ellipse cx="29" cy="39" rx="2.8" ry="1.6" fill="#fdc3d0" opacity="0.85" />
        <ellipse cx="51" cy="39" rx="2.8" ry="1.6" fill="#fdc3d0" opacity="0.85" />

        {/* Olhos grandes redondos pretos com brilho */}
        <ellipse cx="33" cy="34" rx="2.8" ry="3.3" fill="#1a1a22" />
        <ellipse cx="47" cy="34" rx="2.8" ry="3.3" fill="#1a1a22" />
        <ellipse cx="34" cy="32.5" rx="1.1" ry="1.4" fill="#ffffff" />
        <ellipse cx="48" cy="32.5" rx="1.1" ry="1.4" fill="#ffffff" />
        <circle cx="32" cy="35.5" r="0.5" fill="#ffffff" opacity="0.7" />
        <circle cx="46" cy="35.5" r="0.5" fill="#ffffff" opacity="0.7" />

        {/* Nariz triangulo rosa */}
        <path d="M 39 40.5 L 41 40.5 L 40 42 Z" fill="#ff8aa3" />

        {/* Boca sorridente (dois arquinhos pequenos) */}
        <path d="M 40 42 Q 38 44, 36.5 43.3" stroke="#1a1a22" strokeWidth="0.95" fill="none" strokeLinecap="round" />
        <path d="M 40 42 Q 42 44, 43.5 43.3" stroke="#1a1a22" strokeWidth="0.95" fill="none" strokeLinecap="round" />
      </g>
    </svg>
  );
}
