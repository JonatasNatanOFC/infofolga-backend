#!/bin/bash
# Backup diário do banco para o S3. Instalado no servidor em /usr/local/bin e agendado em /etc/cron.d/infofolga-backup.
set -euo pipefail
cd /home/ubuntu/infofolga
f=/tmp/infofolga-$(date +%F).sql.gz
docker compose --env-file .env.production -f docker-compose.prod.yml exec -T banco pg_dump -U infofolga -d infofolga | gzip > "$f"
aws s3 cp "$f" s3://infofolga-teste-backup-549047257997/ --region us-east-2
rm "$f"
