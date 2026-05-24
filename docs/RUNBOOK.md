# Runbook operacional — AcervoLiber em produção

Este documento descreve as operações comuns na infraestrutura de produção: deploy, backup, restore, gestão de usuários, rotação de secrets e troubleshooting.

**Audiência:** quem mantém o servidor (você ou um futuro responsável de TI). Assume familiaridade básica com terminal Linux e Docker.

---

## Visão geral da infra

```
┌─────────────────────────────────────────────────────────────┐
│  VM Oracle Cloud (Ampere A1 ARM, 4 OCPU + 24 GB RAM)        │
│  Ubuntu 22.04 · Docker 24+ · docker compose v2              │
│                                                              │
│  ┌─────────┐  HTTPS  ┌──────────┐  /api  ┌─────────┐        │
│  │  Caddy  │────────▶│ frontend │───────▶│   app   │        │
│  │  80/443 │ TLS LE  │  (nginx) │  proxy │ Spring  │        │
│  └─────────┘         └──────────┘        └─────────┘        │
│                                               │              │
│                                               ▼              │
│                         ┌─────────────┐  ┌─────────┐        │
│                         │   backup    │  │postgres │        │
│                         │ pg_dump 24h │  │   16    │        │
│                         └─────────────┘  └─────────┘        │
└─────────────────────────────────────────────────────────────┘
```

Domínio: `<DOMAIN>.duckdns.org` (atualizado em `.env.prod`).
Volume persistente: `liber-pg-data` (externo — sobrevive a `docker compose down -v`).
Volumes Caddy: `liber-caddy-data` (certificados TLS), `liber-caddy-config`, `liber-caddy-logs`.

OS da VM: **Oracle Linux 9** (ARM aarch64). Usuário SSH default: **`opc`** (não `ubuntu`). Comandos abaixo assumem isso — em caso de troca para Ubuntu, substituir `dnf` por `apt` e `opc` por `ubuntu`.

---

## Primeiro deploy (setup inicial da VM)

Roda 1 vez na vida da VM. Substitua `<DEPLOY_PATH>` por `/home/opc/liber` (ou o que preferir).

```bash
# 1. SSH na VM (Oracle Linux 9 usa o usuario `opc`)
ssh -i ~/.ssh/oracle_liber.key opc@<IP_DA_VM>

# 2. Atualizar OS + instalar Docker (Oracle Linux 9 usa dnf, nao apt)
sudo dnf update -y
sudo dnf config-manager --add-repo=https://download.docker.com/linux/centos/docker-ce.repo
sudo dnf install -y docker-ce docker-ce-cli containerd.io \
  docker-buildx-plugin docker-compose-plugin git
sudo systemctl enable --now docker
sudo usermod -aG docker opc
# Faca logout e login novamente para o grupo `docker` valer (ou: newgrp docker)

# 2.1. Abrir firewall do OS (Oracle Linux vem com firewalld ativo bloqueando 80/443)
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=443/tcp
sudo firewall-cmd --reload
# Lembrete: tambem precisa abrir 80/443 na Security List da subnet pela console Oracle

# 3. Clonar repo
cd ~ && git clone https://github.com/<SEU-USER>/AcervoLiber.git liber
cd liber

# 4. Criar volume persistente do banco
docker volume create liber-pg-data

# 5. Criar diretorio de backups
mkdir -p backups

# 6. Criar .env.prod a partir do template e preencher
cp .env.prod.example .env.prod
nano .env.prod   # AJUSTAR: JWT_SECRET, POSTGRES_PASSWORD, ADMIN_*, DOMAIN, ACME_EMAIL

# 7. Configurar DuckDNS para apontar o IP da VM
# Em https://www.duckdns.org/, pegue seu token e atualize:
curl "https://www.duckdns.org/update?domains=<SEU_SUBDOMINIO>&token=<TOKEN>&ip="

# 8. Primeiro up (vai puxar imagens do GHCR — ja deve ter rodado o CI antes)
docker compose -f docker-compose.yml -f docker-compose.prod.yml \
  --env-file .env.prod pull

docker compose -f docker-compose.yml -f docker-compose.prod.yml \
  --env-file .env.prod up -d

# 9. Verificar
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f app
```

Caddy obtém o certificado TLS automaticamente na primeira request — pode levar 30s no primeiro acesso a `https://<DOMAIN>`.

### Cron para manter DuckDNS atualizado (IP da VM Oracle pode mudar)

```bash
# Crontab da VM:
(crontab -l 2>/dev/null; echo "*/5 * * * * curl -s 'https://www.duckdns.org/update?domains=<SUB>&token=<TOKEN>&ip=' >/dev/null") | crontab -
```

---

## Deploy de nova versão

**Automático:** push para `main` no GitHub → workflow `deploy.yml` builda imagens ARM64, faz push no GHCR e roda `docker compose pull && up -d` via SSH. Acompanhe em **Actions → Deploy**.

**Manual** (em caso de hotfix sem CI):

```bash
ssh -i ~/.ssh/oracle_liber.key opc@<IP_DA_VM>
cd ~/liber
git pull --ff-only origin main
docker compose -f docker-compose.yml -f docker-compose.prod.yml \
  --env-file .env.prod pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml \
  --env-file .env.prod up -d --remove-orphans
```

---

## Backup e restore

### Backup automático

O serviço `backup` no `docker-compose.prod.yml` roda `scripts/backup.sh` a cada 24h. Dumps ficam em `<DEPLOY_PATH>/backups/liber-YYYY-MM-DD_HHMM.sql.gz` com rotação de 30 dias.

### Verificar saúde dos backups

```bash
# Ultimos backups
ls -lh ~/liber/backups/ | tail

# Tamanho deve ser parecido entre dias (variacao pequena = OK)
# Se algum estiver com 0 bytes ou muito menor que os outros, investigar
docker logs liber-backup --tail 50
```

### Restore manual

```bash
# 1. Para o backend (mantem postgres rodando)
docker compose -f docker-compose.yml -f docker-compose.prod.yml stop app

# 2. Restaura o dump (substitui o banco inteiro — CUIDADO)
gunzip < backups/liber-2026-05-23_0400.sql.gz | \
  docker exec -i liber-pg psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}"

# 3. Sobe o backend de novo
docker compose -f docker-compose.yml -f docker-compose.prod.yml \
  --env-file .env.prod up -d app
```

### Backup off-site (recomendado)

O `backup.sh` tem uma linha comentada para upload via `rclone`. Configurar uma vez:

```bash
docker exec -it liber-backup sh -c "apk add --no-cache rclone && rclone config"
# Configurar B2 (Backblaze): bucket gratis ate 10 GB
```

Depois descomente a linha do `rclone copy` no `scripts/backup.sh` e faça `docker compose restart backup`.

---

## Gestão de usuários

### Criar bibliotecário

Não há UI de "criar primeiro usuário" — só ADMIN cria usuários.

```bash
# Logado como ADMIN (via curl ou Postman/Insomnia)
curl -X POST https://<DOMAIN>/api/v1/usuarios \
  -H "Authorization: Bearer <ACCESS_TOKEN_DO_ADMIN>" \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Maria Bibliotecaria",
    "email": "maria@suaescola.edu.br",
    "senha": "<SENHA_PROVISORIA_FORTE>",
    "role": "BIBLIOTECARIO"
  }'
```

O bibliotecário recebe `deveTrocarSenha=true` e é obrigado a trocar no primeiro login (fluxo `/primeiro-acesso` no frontend).

### Destravar conta admin (esqueceu senha / bloqueada por tentativas)

Hard reset direto no banco. **Só faça se realmente perdeu acesso** — bypassa toda a trilha.

```bash
docker exec -it liber-pg psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}"
```

```sql
-- 1. Ver o usuario admin
SELECT id, email, ativo, deve_trocar_senha FROM usuarios WHERE role = 'ADMIN';

-- 2. Reativar (se estava desativado) e marcar "deve trocar senha"
UPDATE usuarios
SET ativo = true,
    deve_trocar_senha = true,
    password_changed_at = NOW()
WHERE email = 'admin@suaescola.edu.br';

-- 3. Definir uma senha provisoria conhecida (BCrypt hash de 'Trocar@2026Forte!')
-- Gere o hash com: docker exec liber-app java -cp app.jar \
--   org.springframework.security.crypto.bcrypt.BCrypt 'Trocar@2026Forte!'
-- Ou use qualquer ferramenta online de BCrypt cost 12
UPDATE usuarios
SET senha_hash = '$2a$12$<HASH_GERADO>'
WHERE email = 'admin@suaescola.edu.br';

-- 4. Revogar todos os refresh tokens do admin (forca relogin)
UPDATE refresh_tokens
SET revoked_at = NOW()
WHERE usuario_id = (SELECT id FROM usuarios WHERE email = 'admin@suaescola.edu.br')
  AND revoked_at IS NULL;
```

Loga, troca a senha imediatamente via UI.

### Lockout por IP+email (15 min)

Se a conta legítima estiver bloqueada por tentativas falhas (HTTP 423):

```sql
-- O lockout e in-memory (nao tem tabela). Reinicia o backend pra zerar.
docker compose -f docker-compose.yml -f docker-compose.prod.yml restart app
```

---

## Rotação de secrets

### JWT_SECRET

Rotacionar invalida **todos** os tokens existentes — todos os usuários precisam relogar. Faça em horário de baixo uso.

```bash
# 1. Gera novo secret
NEW_SECRET=$(openssl rand -base64 48)

# 2. Atualiza no .env.prod
sed -i "s|^JWT_SECRET=.*|JWT_SECRET=${NEW_SECRET}|" ~/liber/.env.prod

# 3. Recria so o container app (postgres mantem)
docker compose -f docker-compose.yml -f docker-compose.prod.yml \
  --env-file .env.prod up -d --force-recreate app
```

### POSTGRES_PASSWORD

Mais delicado — exige update no Postgres E no backend.

```bash
# 1. Trocar senha no Postgres
docker exec -it liber-pg psql -U postgres -c \
  "ALTER USER liber_app WITH PASSWORD '<NOVA_SENHA>';"

# 2. Atualizar .env.prod
sed -i "s|^POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=<NOVA_SENHA>|" ~/liber/.env.prod

# 3. Recriar app
docker compose -f docker-compose.yml -f docker-compose.prod.yml \
  --env-file .env.prod up -d --force-recreate app
```

---

## Migrations

Flyway aplica migrations no boot do backend. Histórico em `flyway_schema_history`.

```bash
# Ver historico
docker exec -it liber-pg psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" \
  -c "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

**REGRA CRÍTICA:** nunca editar migration já aplicada. Migrations aplicadas têm `success=true`. Para corrigir, crie uma nova `V<N+1>__corrige_X.sql`.

Se uma migration falhar no boot, o app não sobe. Investigar:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml logs app --tail 100
```

---

## Monitoramento básico

### Saúde dos containers

```bash
# Status + healthcheck
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps

# Recursos (RAM, CPU)
docker stats --no-stream
```

### Logs

```bash
# Backend (logs JSON em prod profile)
docker logs liber-app --tail 100 -f

# Postgres
docker logs liber-pg --tail 50

# Caddy (incluindo acesso TLS)
docker exec liber-caddy tail -f /var/log/caddy/access.log

# Backup
docker logs liber-backup --tail 50
```

### Auditoria de eventos de segurança

A trilha de auditoria do app fica na tabela `audit_log`:

```sql
-- Logins falhos das ultimas 24h
SELECT created_at, ator_email, ip, detalhe
FROM audit_log
WHERE evento = 'LOGIN_FALHA'
  AND created_at > NOW() - INTERVAL '24 hours'
ORDER BY created_at DESC;

-- Tudo do admin (super crítico)
SELECT created_at, evento, ator_email, detalhe
FROM audit_log
WHERE ator_email LIKE '%admin@%'
ORDER BY created_at DESC LIMIT 100;
```

---

## Troubleshooting

### Caddy não obtém certificado TLS

1. DNS do DuckDNS aponta pro IP da VM? `dig <DOMAIN>` (espera ver IP da VM)
2. Porta 80 aberta na Security List da Oracle? Let's Encrypt usa HTTP-01 challenge.
3. `docker logs liber-caddy` mostra o erro do ACME.
4. Para testar com staging do LE (sem rate limit), descomente `acme_ca` no `Caddyfile`.

### Backend não sobe após deploy

```bash
docker logs liber-app --tail 200
```

Causas comuns:
- Migration Flyway falhou (cria V14__fix.sql)
- `JWT_SECRET` ainda é o default em prod → `IllegalStateException` esperado
- `CORS_ALLOWED_ORIGINS` vazio em prod
- Postgres não subiu (`liber-pg` healthcheck falhando)

### Volume `liber-pg-data` foi deletado por engano

Restaura do backup mais recente:

```bash
docker volume create liber-pg-data
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d postgres
sleep 30  # esperar healthcheck
gunzip < backups/<dump-mais-recente>.sql.gz | \
  docker exec -i liber-pg psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}"
docker compose -f docker-compose.yml -f docker-compose.prod.yml \
  --env-file .env.prod up -d
```

### "Out of capacity" na Oracle ao recriar a VM

A Oracle frequentemente nega criação de VM ARM Always Free por capacidade. Soluções:

1. Retry algumas horas depois (capacidade libera em ciclos)
2. Mudar região (mas perde Block Storage Always Free na home region)
3. Script de retry: https://github.com/hitrov/oci-arm-host-capacity

### Oracle reclama VM ociosa nas férias

Em períodos de baixo uso (julho / dez-jan), a VM pode ser reclamada se CPU/rede/RAM < 20% por 7 dias.

Mitigação simples — cron na VM que faz CPU "trabalhar" 1 min a cada hora:

```bash
(crontab -l 2>/dev/null; echo "0 * * * * timeout 60 sh -c 'while true; do :; done' >/dev/null") | crontab -
```

(Hacky mas funciona. Alternativa: aceitar e ter `.env.prod` + backup atualizado pra subir nova VM rápido.)

---

## Checklist mensal (recomendado)

- [ ] Verificar que dumps de `backups/` estão sendo gerados diariamente e com tamanho consistente
- [ ] Conferir últimas 30 entradas de `audit_log` com `evento IN ('LOGIN_FALHA', 'LOGIN_BLOQUEADO', 'ACESSO_NEGADO', 'REFRESH_REUSO')` — sinais de ataque
- [ ] `docker system df` — limpeza se ocupação > 80% (`docker image prune -af --filter "until=720h"`)
- [ ] Verificar se renovação do certificado TLS rolou (Caddy renova sozinho a cada 60d, mas valida)
- [ ] Aplicar updates de OS: `sudo dnf update -y && sudo reboot` (em horário de baixo uso)
- [ ] Atualizar imagens base: novo deploy via `main` re-puxa `postgres:16-alpine`, `caddy:2-alpine`, etc.
