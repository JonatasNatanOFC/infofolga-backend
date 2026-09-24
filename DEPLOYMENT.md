# Deploy em Produção — InfoFolga API

## Visão geral

Deployment via Docker Compose usando `docker-compose.prod.yml`. Presupõe nginx como reverse proxy (SSL/TLS) em execução no host, roteando para a API em `http://127.0.0.1:8080`.

## Pré-requisitos

1. **Domínio registrado** com A record apontando para IP do servidor
2. **nginx instalado no host** com configuração de proxy reverso
3. **Let's Encrypt SSL** (via Certbot) — configurado no nginx
4. **Docker Engine + Compose plugin** — `docker-compose --version` funciona
5. **Script SQL de schema** — gerado a partir do modelo JPA, já executado no banco de produção (ou será executado antes do primeiro deploy)
6. **Primeiro usuário CEO** — inserido manualmente no banco via SQL

## Preparar ambiente

### 1. Copiar template de env

```bash
cp .env.production.example .env.production
```

### 2. Preencher .env.production com valores reais

```bash
# Editar .env.production e substituir placeholders:
nano .env.production
```

**Gerar senhas fortes:**

```bash
# PostgreSQL password
openssl rand -base64 32

# JWT secret
openssl rand -base64 32
```

**Resultado esperado em .env.production:**

```
PORT=8080
POSTGRES_PASSWORD=AbCdEf123456789...
JWT_SECRET=XyZ789AbCdEf123456789...
API_BIND=127.0.0.1
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_PROFILES_ACTIVE=prod
```

### 3. Configurar nginx (no host)

Criar `/etc/nginx/sites-available/infofolga-api`:

```nginx
server {
    listen 443 ssl http2;
    server_name api.seudominio.com;

    ssl_certificate /etc/letsencrypt/live/api.seudominio.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.seudominio.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 30s;
    }
}

server {
    listen 80;
    server_name api.seudominio.com;
    return 301 https://$host$request_uri;
}
```

Habilitar e recarregar:

```bash
ln -s /etc/nginx/sites-available/infofolga-api /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx
```

### 4. Inicializar banco de dados

Se é a primeira vez subindo em produção:

```bash
# Conectar ao banco (postgres rodará no container)
# Executar script SQL manualmente ou via docker-compose exec

# OU deixar o Hibernate criar com ddl-auto=create (UMA VEZ APENAS)
# Depois mudar para validate em .env.production
```

Inserir primeiro CEO (após container estar rodando):

```bash
docker-compose -f docker-compose.prod.yml exec banco psql -U infofolga -d infofolga -c "
INSERT INTO colaboradores (nome, cpf, email, senha, cargo, setor, role, status)
VALUES ('CEO Inicial', '12345678901', 'ceo@empresa.com', '\$2a\$10\$...', 'CEO', 'Admin', 'CEO', 'ativo');
"
```

(A senha acima deve ser um hash BCrypt válido; gerar via `https://bcrypt.online` ou script Java)

## Deploy

### Primeiro deploy (com build local)

```bash
docker-compose -f docker-compose.prod.yml up -d
docker-compose -f docker-compose.prod.yml logs -f api
```

Aguardar mensagem `Tomcat started on port 8080`.

### Deploys subsequentes (atualizar código)

```bash
# Pull latest image
docker pull <seu-registry>/infofolga-api:latest

# Rebuild e reiniciar
docker-compose -f docker-compose.prod.yml up -d --build

# Ou, se usando imagem pré-built
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d
```

## Validação pós-deploy

```bash
# Verificar containers em execução
docker-compose -f docker-compose.prod.yml ps

# Testar login via curl
curl -X POST https://api.seudominio.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"cpf":"12345678901","senha":"senha"}'

# Resultado esperado: token JWT
# { "token": "eyJ...", "nome": "CEO Inicial", "role": "CEO" }

# Testar GET autenticado
curl -H "Authorization: Bearer eyJ..." https://api.seudominio.com/api/colaboradores/me
```

## Backup

Agendar `pg_dump` diário:

```bash
# /usr/local/bin/backup-infofolga.sh
#!/bin/bash
docker-compose -f /path/to/docker-compose.prod.yml exec -T banco \
  pg_dump -U infofolga infofolga \
  | gzip > /backups/infofolga_$(date +\%Y\%m\%d).sql.gz

# Validar backup
gzip -t /backups/infofolga_$(date +\%Y\%m\%d).sql.gz && echo "OK" || echo "FALHA"
```

Adicionar ao crontab:

```bash
0 2 * * * /usr/local/bin/backup-infofolga.sh
```

## Rollback

Se um deploy quebrou a aplicação:

```bash
# Reverter para imagem anterior
docker-compose -f docker-compose.prod.yml down
docker rmi <seu-registry>/infofolga-api:latest
docker-compose -f docker-compose.prod.yml up -d
```

Se o schema foi alterado e precisa de rollback:

```bash
# Restaurar banco de backup
gzip -d < /backups/infofolga_20240101.sql.gz | \
  docker-compose -f docker-compose.prod.yml exec -T banco \
    psql -U infofolga infofolga
```

## Troubleshooting

**API não responde em http://127.0.0.1:8080**
- `docker-compose -f docker-compose.prod.yml logs api` — verificar erro de startup
- `docker-compose -f docker-compose.prod.yml exec api netstat -tulpn | grep 8080` — verificar porta

**nginx retorna 502 Bad Gateway**
- `nginx -t` — validar configuração nginx
- `docker-compose -f docker-compose.prod.yml ps` — verificar se containers estão UP
- `curl -v http://127.0.0.1:8080/error` — testar connectivity interna

**Login falha com 401**
- Verificar JWT_SECRET em .env.production — JWT e autenticação usam mesmo secret
- Verificar que CEO foi inserido no banco

**Banco não inicia**
- `docker-compose -f docker-compose.prod.yml logs banco` — erros de postgres
- Volume `infofolga_postgres_data_prod` pode estar corrupto; backup/restore necessário

## Monitoramento

Logs em tempo real:

```bash
docker-compose -f docker-compose.prod.yml logs -f api
```

Salvar logs em arquivo:

```bash
docker-compose -f docker-compose.prod.yml logs api > /var/log/infofolga-api.log 2>&1
```

Ou configurar Docker para enviar logs via journald:

```bash
# Editar /etc/docker/daemon.json
{
  "log-driver": "journald"
}
# systemctl restart docker
# journalctl -u docker -f
```
