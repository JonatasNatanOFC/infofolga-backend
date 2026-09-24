# Deploy em Produção — InfoFolga API

## Visão geral

Deployment via Docker Compose usando `docker-compose.prod.yml`. Presupõe nginx como reverse proxy (SSL/TLS) em execução no host, roteando para a API em `http://127.0.0.1:8080`.

## Pré-requisitos

1. **Domínio registrado** com A record apontando para IP do servidor
2. **nginx instalado no host** com configuração de proxy reverso
3. **Let's Encrypt SSL** (via Certbot) — configurado no nginx
4. **Docker Engine + Compose plugin** — `docker compose --version` funciona
5. **Script SQL de schema** — `database/V1__schema_inicial.sql` será executado no banco vazio
6. **Primeiro usuário CEO** — será inserido manualmente no banco via SQL

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

**Verificar que .env.production contém:**

```
PORT=8080
POSTGRES_DB=infofolga
POSTGRES_USER=infofolga
POSTGRES_PASSWORD=<senha_forte_gerada_com_openssl>
SPRING_DATASOURCE_URL=jdbc:postgresql://banco:5432/infofolga
SPRING_DATASOURCE_USERNAME=infofolga
SPRING_DATASOURCE_PASSWORD=<senha_forte_gerada_com_openssl>
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
JWT_SECRET=<secret_forte_gerado_com_openssl>
API_BIND=127.0.0.1
SPRING_PROFILES_ACTIVE=prod
```

**NUNCA commit .env.production. Está no .gitignore.**

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

### 4. Preparar banco de dados

**Ordem obrigatória de execução:**

1. Banco PostgreSQL deve estar vazio
2. Executar `database/V1__schema_inicial.sql` (uma única vez)
3. Validar schema
4. Subir API com `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`

**Passo 1: Subir apenas o banco (sem a API)**

```bash
docker compose -f docker-compose.prod.yml up -d banco
```

Aguardar healthcheck passar (retorna "healthy"):

```bash
docker compose -f docker-compose.prod.yml ps banco
```

**Passo 2: Executar script SQL inicial no banco vazio**

```bash
cat database/V1__schema_inicial.sql | docker compose -f docker-compose.prod.yml exec -T banco psql -U infofolga -d infofolga
```

Não deve haver erros.

**Passo 3: Validar que as tabelas foram criadas**

```bash
docker compose -f docker-compose.prod.yml exec banco psql -U infofolga -d infofolga -c "\dt"
```

Saída esperada:

```
                List of relations
 Schema |     Name      | Type  |    Owner
--------+---------------+-------+----------
 public | colaboradores | table | infofolga
 public | solicitacoes  | table | infofolga
(2 rows)
```

Verificar índices:

```bash
docker compose -f docker-compose.prod.yml exec banco psql -U infofolga -d infofolga -c "\di"
```

Devem estar presentes: `idx_solicitacoes_colaborador_id`, `idx_solicitacoes_aprovador_id`, `idx_solicitacoes_status`, `idx_solicitacoes_tipo_status`, `idx_solicitacoes_data_inicio_data_fim`, `idx_solicitacoes_atualizado_em`.

**Passo 4: Subir a API**

```bash
docker compose -f docker-compose.prod.yml up -d api
```

**Passo 5: Monitorar logs da API**

```bash
docker compose -f docker-compose.prod.yml logs -f api
```

Aguardar mensagem como:

```
Started InfofolgaApiApplication in X.XX seconds
```

**Crítico:** Nenhuma mensagem `SchemaManagementException` deve aparecer. Se aparecer, o schema real diverge do mapeamento JPA — PARE e não prossiga.

**Passo 6: Testar conectividade da API**

```bash
curl -I https://api.seudominio.com/api/auth
```

Deve responder com `200` ou `401` (esperado, sem token).

**Passo 7: Inserir primeiro CEO no banco**

```bash
docker compose -f docker-compose.prod.yml exec banco psql -U infofolga -d infofolga -c "
INSERT INTO colaboradores (nome, cpf, email, senha, cargo, setor, role, status)
VALUES ('CEO Inicial', '00000000001', 'ceo@empresa.com', '\$2a\$10\$HASH_BCRYPT_AQUI', 'CEO', 'Administração', 'CEO', 'ativo');
"
```

**A senha deve ser um hash BCrypt válido gerado de forma segura no seu ambiente.** Não use serviços online para gerar senhas de produção.

Para gerar o hash localmente em Java:

```bash
# Em um projeto Java com Spring Security, use:
# org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
# System.out.println(encoder.encode("sua_senha_segura"));
```

Ou via Linux com ferramentas adequadas para seu ambiente.

**Passo 8: Testar login**

```bash
curl -X POST https://api.seudominio.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"cpf":"00000000001","senha":"sua_senha_aqui"}'
```

Deve retornar um token JWT.

## Deploy

### Primeiro deploy (com build local)

**Pré-requisitos verificados:**
- ✅ `.env.production` preenchido com valores reais
- ✅ Schema SQL já executado (`database/V1__schema_inicial.sql`)
- ✅ nginx configurado e rodando com SSL
- ✅ `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` em `.env.production`
- ✅ Banco PostgreSQL subido e saudável
- ✅ Primeiro CEO inserido no banco

```bash
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml logs -f api
```

Aguardar `Started InfofolgaApiApplication in X.XX seconds`. Nenhuma `SchemaManagementException`.

### Deploys subsequentes (atualizar código)

Rebuild e reiniciar:

```bash
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml logs -f api
```

Ou, se usando imagem pré-built de um registry:

```bash
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml logs -f api
```

**Importante:** O Hibernate permanecerá com `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`. Se o código incluir alterações de schema JPA (novas entidades, mudança de tipos, constraints), a API falhará no startup com `SchemaManagementException`.

Nesse caso:
1. Crie um novo script SQL (ex: `database/V2__alteracao.sql`) com a alteração de schema
2. Execute o script manualmente no banco em produção
3. Faça o deploy do código novo

## Validação pós-deploy

Verificar status dos containers:

```bash
docker compose -f docker-compose.prod.yml ps
```

Todos devem estar em `Up`.

Verificar saúde do banco:

```bash
docker compose -f docker-compose.prod.yml exec banco pg_isready -U infofolga
```

Resultado esperado: `/var/run/postgresql:5432 - accepting connections`

Monitorar logs:

```bash
docker compose -f docker-compose.prod.yml logs -f
```

## Backup

Agendar `pg_dump` diário:

```bash
# /usr/local/bin/backup-infofolga.sh
#!/bin/bash
docker compose -f /path/to/docker-compose.prod.yml exec -T banco \
  pg_dump -U infofolga infofolga \
  | gzip > /backups/infofolga_$(date +\%Y\%m\%d).sql.gz

# Validar backup
gzip -t /backups/infofolga_$(date +\%Y\%m\%d).sql.gz && echo "OK" || echo "FALHA"
```

Adicionar ao crontab:

```bash
0 2 * * * /usr/local/bin/backup-infofolga.sh
```

Testar restore periodicamente em banco de teste.

## Rollback

Se um deploy quebrou a aplicação:

```bash
docker compose -f docker-compose.prod.yml down
# Remover imagem antiga se necessário
docker image rm <seu-registry>/infofolga-api:latest
# Subir novamente (Docker puxará a imagem anterior ou rebuild)
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml logs -f api
```

Se o schema foi alterado e precisa de rollback:

```bash
# Restaurar banco de um backup
gzip -d < /backups/infofolga_20260101.sql.gz | \
  docker compose -f docker-compose.prod.yml exec -T banco \
    psql -U infofolga infofolga
```

## Troubleshooting

**API não responde via nginx**
- `curl -v http://127.0.0.1:8080/error` — verificar se API está respondendo localmente
- `nginx -t` — validar configuração nginx
- `docker compose -f docker-compose.prod.yml logs api` — verificar erro de startup

**Banco não inicia**
- `docker compose -f docker-compose.prod.yml logs banco` — erros de PostgreSQL
- `docker compose -f docker-compose.prod.yml ps banco` — verificar se healthcheck passa

**Erro SchemaManagementException ao subir API**
- Schema real diverge do mapeamento JPA
- Verificar: `docker compose -f docker-compose.prod.yml logs api | grep -iE "schema|diverge"`
- Solução: executar migration SQL (ex: `V2__...sql`) manualmente, depois redeploy código

**Conexão recusada na porta 8080**
- `docker compose -f docker-compose.prod.yml exec api netstat -tulpn | grep 8080` — verificar porta
- `docker compose -f docker-compose.prod.yml ps api` — verificar se container está UP

**JWT_SECRET inválido**
- JWT_SECRET deve ter mínimo 32 bytes
- Gerar novamente: `openssl rand -base64 32`
- Alterar em `.env.production` e fazer redeploy

**Banco está cheio**
- `docker compose -f docker-compose.prod.yml exec banco du -h /var/lib/postgresql/data`
- Limpar backups antigos de `/backups/`
- Se necessário, vacuum no PostgreSQL: `docker compose -f docker-compose.prod.yml exec banco vacuumdb -U infofolga infofolga`
