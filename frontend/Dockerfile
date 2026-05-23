# =============================================================================
# Dockerfile do frontend — build com Node, serve com Nginx (imagem final enxuta)
# =============================================================================

# ---- Estagio 1: build ----
FROM node:22-alpine AS build
WORKDIR /app

# Instala dependencias primeiro (camada cacheada enquanto o lockfile nao muda)
COPY package.json package-lock.json ./
RUN npm ci

# Build da aplicacao. VITE_API_URL vazio => o frontend chama /api/v1 (mesma
# origem) e o Nginx faz proxy para o backend — sem CORS.
ARG VITE_API_URL=""
ENV VITE_API_URL=${VITE_API_URL}
COPY . .
RUN npm run build

# ---- Estagio 2: runtime ----
# Usamos a imagem unprivileged oficial: roda como usuario `nginx` (UID 101) por
# padrao e escuta na porta 8080. Se houver RCE no nginx (parse exotico de header),
# o atacante NAO tem root no container — hardening basico.
FROM nginxinc/nginx-unprivileged:1.27-alpine AS runtime

# Config do Nginx (SPA + proxy /api -> backend). O arquivo escuta na 8080 — vide
# nginx.conf — para combinar com o usuario nao-root (portas <1024 exigem root).
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Artefatos estaticos do build
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080
# 127.0.0.1 (nao "localhost") — o nginx escuta so em IPv4; localhost resolve ::1
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/ >/dev/null 2>&1 || exit 1

CMD ["nginx", "-g", "daemon off;"]
