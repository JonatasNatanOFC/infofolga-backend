# Database — InfoFolga

## Arquivo de schema

**`V1__schema_inicial.sql`** — Schema inicial de produção.

Reproduz exatamente o mapeamento JPA (Colaborador, Solicitacao). Compatível com `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`.

## Como executar em um banco PostgreSQL vazio

### Pré-requisito

O banco deve estar vazio ou sem tabelas do InfoFolga. Em produção, nunca re-executar este script em um banco com dados.

### Execução

```bash
# Via psql com banco rodando em container
docker compose --env-file .env.production -f docker-compose.prod.yml exec banco psql -U infofolga -d infofolga < database/V1__schema_inicial.sql

# OU, local contra banco em container
psql -h localhost -p 5441 -U infofolga -d infofolga < database/V1__schema_inicial.sql

# OU, direto no container via COPY (stdin)
cat database/V1__schema_inicial.sql | docker compose --env-file .env.production -f docker-compose.prod.yml exec -T banco psql -U infofolga -d infofolga
```

### Validar execução

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml exec banco psql -U infofolga -d infofolga -c "
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' ORDER BY table_name;
"

# Saída esperada:
# colaboradores
# solicitacoes
```

Inspecionar schema de uma tabela:

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml exec banco psql -U infofolga -d infofolga -c "\d colaboradores"
```

## Como iniciar a API com `ddl-auto=validate`

1. **Garantir que o script SQL foi executado:**

```bash
# Rodar script
docker compose --env-file .env.production -f docker-compose.prod.yml exec banco psql -U infofolga -d infofolga < database/V1__schema_inicial.sql
```

2. **Verificar `.env.production`:**

```bash
# .env.production deve ter:
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_PROFILES_ACTIVE=prod
```

3. **Subir a API:**

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml up -d api

# Monitorar logs
docker compose --env-file .env.production -f docker-compose.prod.yml logs -f api
```

4. **Verificar startup:**

Se não houver erros, logs mostrarão:

```
... Tomcat started on port 8080
... Started InfofolgaApiApplication
```

## Erros esperados do Hibernate se houver divergência

Se o schema não corresponder ao mapeamento JPA, o Hibernate lançará `SchemaManagementException` na inicialização:

**Coluna faltando:**
```
ERROR: table "solicitacoes" has no column named "data_inicio"
SchemaManagementException: Schema validation: missing column [data_inicio] in table [solicitacoes]
```

**Coluna com tipo errado:**
```
ERROR: column "criado_em" is of type timestamp without time zone but should be timestamp without time zone
SchemaManagementException: Schema validation: wrong column type [criado_em] in table [solicitacoes]
```

**Constraint (NOT NULL) divergente:**
```
ERROR: column "tipo" is nullable but should not be
SchemaManagementException: Schema validation: column [tipo] in table [solicitacoes] has NOT NULL constraint but column has no NOT NULL
```

**FK faltando:**
```
ERROR: no constraint named "fk_colaborador" in table "solicitacoes"
SchemaManagementException: Schema validation: constraint [fk_colaborador] missing in table [solicitacoes]
```

Nesses casos, **PARE e não avance**. Verifique:
- Se o script foi executado completamente
- Se não houve DROP/DELETE de tabelas entre execução do script e startup da API
- Se as entidades JPA foram alteradas depois da criação deste script

## Inserir primeiro CEO

Após validar que a API subiu com sucesso:

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml exec banco psql -U infofolga -d infofolga

-- No psql:
-- Gerar hash BCrypt de uma senha (ex: "senha123"):
-- Use https://bcrypt.online ou:
-- echo -n "senha123" | htpasswd -bnBC 10 "" | tr -d ':\n'

INSERT INTO colaboradores (nome, cpf, email, senha, cargo, setor, role, status)
VALUES ('CEO Inicial', '00000000001', 'ceo@empresa.com', '$2a$10$YOUR_BCRYPT_HASH_HERE', 'CEO', 'Administração', 'CEO', 'ativo');
```

## Divergências encontradas

**Nenhuma.** O arquivo `V1__schema_inicial.sql` corresponde exatamente ao mapeamento das entidades JPA:

- Nomes de tabelas e colunas conferem (camelCase JPA → snake_case SQL)
- Tipos de dados: String → VARCHAR(255), LocalDate → DATE, LocalDateTime → TIMESTAMP, Long → BIGINT, Enums → VARCHAR
- Constraints: UNIQUE em cpf e email, NOT NULL em tipo, status, data_inicio, data_fim, criado_em, versao
- Foreign Keys: colaborador_id e aprovador_id → colaboradores(id)
- Índices: adicionados nas FKs e colunas frequentemente filtradas (status, tipo, data_inicio, data_fim, atualizado_em)

## Próximos passos

1. Executar `V1__schema_inicial.sql` no banco vazio (antes de subir a API)
2. Verificar schema com `\d` no psql
3. Subir a API com `docker compose --env-file .env.production -f docker-compose.prod.yml up -d`
4. Monitorar logs para erros de SchemaManagementException
5. Se OK, inserir primeiro CEO manualmente via SQL
6. Testar login via curl/Postman
7. Proceder para nginx + SSL (próxima etapa)

## Plano futuro

**V1 é o schema inicial de produção.** Quando o schema precisar evoluir:

1. **Criar migration versionada:** `V2__adicionar_coluna_x.sql`, `V3__renomear_tabela.sql`, etc.
2. **Workflow de migrations:**
   - Criar arquivo `database/V<N>__<descricao>.sql` com alterações
   - Testar em banco de desenvolvimento
   - Executar manualmente em produção **antes** de subir a API com novas entidades JPA
   - Verificar que `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` ainda passa sem erros
3. **Ferramentas recomendadas no futuro:** quando múltiplas migrations acumularem, adotar **Flyway** para orquestração automática de migrations versionadas.

**Por agora (primeiro deploy):** schema V1 é suficiente. Migrations serão adicionadas conforme o projeto evoluir.
