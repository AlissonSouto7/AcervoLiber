#!/bin/sh
# =============================================================================
# backup.sh — pg_dump diario com rotacao de 30 dias
# =============================================================================
# Roda dentro do container 'backup' do docker-compose.prod.yml.
# Conecta no servico 'postgres' via rede interna do compose.
#
# Volume montado em /backups (no host: ./backups) guarda os dumps comprimidos.
#
# Para restaurar:
#   gunzip < backups/liber-YYYY-MM-DD.sql.gz | docker exec -i liber-pg \
#     psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
# =============================================================================

set -eu

BACKUP_DIR=/backups
RETENTION_DAYS=30
TIMESTAMP=$(date +%Y-%m-%d_%H%M)
BACKUP_FILE="$BACKUP_DIR/liber-$TIMESTAMP.sql.gz"

echo "[backup] iniciando dump em $BACKUP_FILE"

# pg_dump custom format (-Fc) seria mais flexivel, mas plain + gzip e mais
# simples de inspecionar e restaurar. Compressao -9 economiza espaco.
pg_dump \
  -h postgres \
  -U "$POSTGRES_USER" \
  -d "$POSTGRES_DB" \
  --no-owner \
  --no-acl \
  --clean \
  --if-exists \
  | gzip -9 > "$BACKUP_FILE"

# Confirma que o arquivo nao esta vazio (defesa contra falha silenciosa)
if [ ! -s "$BACKUP_FILE" ]; then
  echo "[backup] ERRO: arquivo vazio, removendo"
  rm -f "$BACKUP_FILE"
  exit 1
fi

SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "[backup] concluido: $BACKUP_FILE ($SIZE)"

# Rotacao — apaga dumps com mais de $RETENTION_DAYS dias
DELETED=$(find "$BACKUP_DIR" -name "liber-*.sql.gz" -type f -mtime +"$RETENTION_DAYS" -print -delete | wc -l)
if [ "$DELETED" -gt 0 ]; then
  echo "[backup] rotacao: $DELETED dump(s) antigos removidos"
fi

# Opcional: upload para Backblaze B2 / S3 / Wasabi via rclone.
# Descomente e configure rclone separadamente se quiser backup off-site.
# rclone copy "$BACKUP_FILE" b2-remote:liber-backups/ --quiet

echo "[backup] OK"
