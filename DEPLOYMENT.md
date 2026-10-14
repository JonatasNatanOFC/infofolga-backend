# Deploy em produção

Como o InfoFolga roda hoje e como atualizar sem perder dados.

## Como está montado

```
Internet ──HTTPS──► nginx (EC2) ──┬── /        → app web (arquivos em /var/www/infofolga)
                                  └── /api/... → API (Docker, 127.0.0.1:8080) ──► PostgreSQL (Docker, 127.0.0.1:5441)
                                                                                         │
                                                                    backup diário ───────┴──► S3
```

| Peça | Onde |
|---|---|
| Servidor | AWS EC2 (Ubuntu), projeto em `/home/ubuntu/infofolga` |
| Domínio | `3-13-86-161.sslip.io` (provisório), certificado Let's Encrypt |
| API e banco | `docker-compose.prod.yml`, segredos em `.env.production` (só no servidor) |
| App web | Arquivos estáticos em `/var/www/infofolga` |
| Backup | `scripts/backup-infofolga.sh`, todo dia, para o S3 |

A porta 22 fica fechada. O acesso ao servidor é pelo console da AWS.

## Atualizar a API

```bash
cd /home/ubuntu/infofolga
git pull

# só se o commit trouxe migration nova (ver database/README.md)
sudo /usr/local/bin/backup-infofolga.sh   # backup na hora, antes de mexer no schema
cat database/V8__exemplo.sql | docker compose --env-file .env.production -f docker-compose.prod.yml \
  exec -T banco psql -U infofolga -d infofolga -v ON_ERROR_STOP=1

docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build api
docker compose --env-file .env.production -f docker-compose.prod.yml logs -f api
```

Espere `Started InfofolgaApiApplication`. Se aparecer `SchemaManagementException`, falta aplicar uma migration: a API não sobe com schema divergente, então os dados não correm risco.

**Ordem importa:** migration primeiro, API depois. As migrations só acrescentam tabelas e colunas, então a API antiga continua funcionando com o schema novo durante a troca.

## Atualizar o app web

Na sua máquina, no repositório do app:

```bash
npx expo export -p web --output-dir dist
tar -czf infofolga-web.tar.gz -C dist .
```

Envie `infofolga-web.tar.gz` para a pasta `web/` do bucket de backup pelo console do S3. No servidor:

```bash
aws s3 cp s3://infofolga-teste-backup-549047257997/web/infofolga-web.tar.gz /tmp/
sudo tar -xzf /tmp/infofolga-web.tar.gz -C /var/www/infofolga
```

Para conferir se a versão nova está no ar, compare o hash do bundle em `/var/www/infofolga/index.html` com o de `dist/index.html`. Se o navegador mostrar a versão antiga, recarregue com Ctrl+Shift+R.

## Primeira instalação

1. Instale Docker, nginx e certbot no servidor e clone o repositório em `/home/ubuntu/infofolga`.
2. Copie `.env.production.example` para `.env.production` e preencha. Gere senha e segredo com `openssl rand -base64 32`. Mantenha `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` e `API_BIND=127.0.0.1`.
3. Suba só o banco e espere ficar `healthy`:
   ```bash
   docker compose --env-file .env.production -f docker-compose.prod.yml up -d banco
   docker compose --env-file .env.production -f docker-compose.prod.yml ps banco
   ```
4. Rode as migrations V1 a V7 e crie a empresa e o primeiro CEO ([database/README.md](database/README.md#banco-novo-do-zero)).
5. Suba a API: `docker compose --env-file .env.production -f docker-compose.prod.yml up -d api`.
6. Configure o nginx (abaixo) e o certificado (`certbot --nginx -d seu.dominio`).
7. Publique o app web e instale o backup (abaixo).

### nginx

Modelo da configuração. Antes de mudar o servidor, compare com o arquivo em uso (`/etc/nginx/sites-enabled/`).

```nginx
server {
    listen 443 ssl http2;
    server_name seu.dominio;
    # ssl_certificate e ssl_certificate_key: preenchidos pelo certbot

    root /var/www/infofolga;
    index index.html;
    client_max_body_size 20m;   # mesmo limite de upload da API (AFD)

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $remote_addr;   # sobrescreve: o rate limit usa o primeiro valor
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }

    # o app é uma SPA: qualquer rota que não seja arquivo devolve o index.html
    location / {
        try_files $uri $uri/ /index.html;
    }
}

server {
    listen 80;
    server_name seu.dominio;
    return 301 https://$host$request_uri;
}
```

`X-Forwarded-For $remote_addr` precisa **sobrescrever** o cabeçalho, nunca acrescentar (`$proxy_add_x_forwarded_for`). Senão o cliente escolhe o IP que o rate limit enxerga.

## Backup

`scripts/backup-infofolga.sh` faz `pg_dump`, compacta e envia para o S3. Instalação no servidor:

```bash
sudo cp scripts/backup-infofolga.sh /usr/local/bin/
sudo chmod +x /usr/local/bin/backup-infofolga.sh
echo '0 5 * * * ubuntu /usr/local/bin/backup-infofolga.sh' | sudo tee /etc/cron.d/infofolga-backup
```

O horário do cron é UTC: no exemplo, 5h UTC = 2h em Brasília. Confira o agendamento real com `cat /etc/cron.d/infofolga-backup`.

### Restaurar

Teste a restauração de vez em quando num banco de teste. Um backup que nunca foi restaurado não está provado.

```bash
aws s3 cp s3://infofolga-teste-backup-549047257997/infofolga-AAAA-MM-DD.sql.gz /tmp/
gunzip -c /tmp/infofolga-AAAA-MM-DD.sql.gz | docker compose --env-file .env.production -f docker-compose.prod.yml \
  exec -T banco psql -U infofolga -d infofolga -v ON_ERROR_STOP=1
```

Restaure sobre um banco **vazio** (o dump recria as tabelas). Para restaurar em produção: pare a API, renomeie ou apague o banco atual, crie um vazio e restaure.

## Voltar uma versão

Se a API nova quebrou e **não houve migration**:

```bash
git log --oneline -5
git checkout <commit-anterior>
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build api
git checkout master   # depois de corrigir
```

Se houve migration, a API antiga normalmente continua funcionando (as migrations só acrescentam). Se não funcionar, restaure o backup feito antes da migration.

## Problemas comuns

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| API não sobe, `SchemaManagementException` | Migration não aplicada | Aplicar o script que falta e subir de novo |
| API não sobe, `JWT_SECRET ausente ou fraco` | Segredo com menos de 32 bytes | `openssl rand -base64 32` e atualizar `.env.production` |
| `502 Bad Gateway` | API parada | `docker compose ... ps` e `logs api` |
| Importar AFD dá `413` | Limite do nginx | Conferir `client_max_body_size 20m` |
| App mostra versão antiga | Cache do navegador ou tar não extraído | Comparar o hash em `index.html`, Ctrl+Shift+R |
| Erro de CORS no navegador | Faltou `SERVER_FORWARD_HEADERS_STRATEGY: native` | Já está no `docker-compose.prod.yml`; conferir se não foi removido |

## Nunca

- Commitar `.env.production` ou colar seu conteúdo em chat, issue ou print.
- Abrir a porta 22 ou a 5441 para a internet.
- Rodar a API com `ddl-auto=update` em produção.
- Aplicar migration sem backup.
