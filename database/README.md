# Banco de dados

PostgreSQL 16. O schema é criado e alterado **só por scripts SQL** desta pasta. A API sobe com `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`: se o schema real divergir das entidades JPA, ela se recusa a iniciar (`SchemaManagementException`) em vez de mexer no banco.

## Migrations

Rodar em ordem, uma única vez cada, com a API parada e depois de um backup.

| Script | O que faz |
|---|---|
| `V1__schema_inicial.sql` | Tabelas `colaboradores` e `solicitacoes`, com índices |
| `V2__multiempresa.sql` | Tabela `empresas` e `empresa_id` em colaboradores e solicitações. Os dados existentes passam a ser da empresa 1 (Infoway). Roda numa transação: se algo falhar, nada muda |
| `V3__pontuacao.sql` | `configuracao_pontuacao`, `feriados`, `apuracao_dia`; motivo da folga e `desconta_saldo` nas solicitações |
| `V4__feriado_anual.sql` | Feriado que se repete todo ano (`feriados.anual`) |
| `V5__rh_banco_de_horas.sql` | Se o RH da empresa acumula pontos (`rh_tem_banco_de_horas`) |
| `V6__batidas_e_ajustes.sql` | `batidas` (marcações brutas do ponto) e `ajustes_saldo` (ajustes manuais) |
| `V7__saldo_no_pedido.sql` | `saldo_no_pedido` e `pontos_da_folga` nas solicitações |

Para aplicar uma migration no servidor (a partir de `/home/ubuntu/infofolga`):

```bash
cat database/V7__saldo_no_pedido.sql | docker compose --env-file .env.production -f docker-compose.prod.yml \
  exec -T banco psql -U infofolga -d infofolga -v ON_ERROR_STOP=1
```

`ON_ERROR_STOP=1` faz o `psql` parar no primeiro erro em vez de seguir executando o resto do script.

### Criar uma migration nova

1. Crie `V8__descricao.sql` com o comentário do que faz e por quê no topo.
2. Altere a entidade JPA no mesmo commit.
3. Rode `./mvnw test`. Atenção: os testes de integração criam o schema **a partir das entidades** (`create-drop`), não dos scripts. Eles provam que o código funciona, não que o script está certo.
4. Teste o script num banco local com `ddl-auto=validate`: se a API subir, o script bate com as entidades.
5. Em produção: backup, aplique o script, depois suba a API nova.

## Banco novo, do zero

```bash
for f in database/V*.sql; do
  cat "$f" | docker compose --env-file .env.production -f docker-compose.prod.yml \
    exec -T banco psql -U infofolga -d infofolga -v ON_ERROR_STOP=1 || break
done
```

A V2 já cria a Infoway como empresa 1. Para outra empresa, crie a empresa e o primeiro CEO:

```sql
BEGIN;
INSERT INTO empresas (nome) VALUES ('Nome da Empresa');
INSERT INTO colaboradores (empresa_id, nome, cpf, email, senha, cargo, setor, role, status)
SELECT id, 'Nome do CEO', '00000000000', 'ceo@empresa.com', '<hash-bcrypt>', 'Diretor', 'Administração', 'CEO', 'ativo'
FROM empresas WHERE nome = 'Nome da Empresa';
COMMIT;
```

- CPF só com números.
- Gere o hash BCrypt na sua máquina, nunca num site: `htpasswd -bnBC 10 "" 'senha' | tr -d ':\n'`.
- O logo da empresa (`empresas.logo` e `logo_relatorio`) é um data URI base64 e é opcional.

Daí em diante o CEO cadastra os demais colaboradores pelo app.

## Conferir o schema

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml exec banco psql -U infofolga -d infofolga -c "\dt"
```

Tabelas esperadas: `ajustes_saldo`, `apuracao_dia`, `batidas`, `colaboradores`, `configuracao_pontuacao`, `empresas`, `feriados`, `solicitacoes`.

## Backup e restauração

Ver [DEPLOYMENT.md](../DEPLOYMENT.md#backup).

> `SCHEMA_MAPPING.md` descreve o mapeamento campo a campo das tabelas da V1. Para as tabelas criadas depois, os próprios scripts são a referência.
