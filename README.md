# InfoFolga API

API REST para gestão de **folgas e férias** de colaboradores. Funcionários abrem solicitações, gerentes e CEO aprovam ou rejeitam, e um dashboard consolida os números da equipe.

**Stack:** Java 17 · Spring Boot 3.5 · Spring Security + JWT (Auth0 `java-jwt`) · Spring Data JPA · PostgreSQL 16 · Lombok · Docker

---

## Sumário

- [Como rodar](#como-rodar)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Primeiro acesso](#primeiro-acesso)
- [Autenticação](#autenticação)
- [Perfis de acesso](#perfis-de-acesso)
- [Endpoints](#endpoints)
- [Ciclo de vida de uma solicitação](#ciclo-de-vida-de-uma-solicitação)
- [Erros](#erros)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Testes](#testes)

---

## Como rodar

### Opção 1 — Docker Compose (API + banco)

```bash
# crie o .env na raiz do projeto (veja "Variáveis de ambiente")
docker compose up --build
```

- API: `http://localhost:8080`
- PostgreSQL: `localhost:5441` (porta 5432 dentro da rede do compose)

Dentro do compose, a API acessa o banco pelo host `banco`, então use:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://banco:5432/infofolga_db
```

### Opção 2 — API local, banco no Docker

```bash
docker compose up -d banco
```

Exporte as variáveis (ou configure-as na sua IDE) apontando para a porta exposta:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5441/infofolga_db
export SPRING_DATASOURCE_USERNAME=...
export SPRING_DATASOURCE_PASSWORD=...
export JWT_SECRET=...

./mvnw spring-boot:run
```

> O Spring Boot **não** lê o arquivo `.env` sozinho. Ao rodar fora do Docker, as variáveis precisam estar no ambiente do processo.

Para logs detalhados de segurança e web, ative o perfil `debug`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=debug
```

---

## Variáveis de ambiente

O arquivo `.env` fica na raiz do projeto e está no `.gitignore` — **nunca faça commit dele**.

| Variável | Uso | Exemplo |
|---|---|---|
| `PORT` | Porta HTTP da API (padrão `8080`) | `8080` |
| `POSTGRES_DB` | Nome do banco criado pelo container | `infofolga_db` |
| `POSTGRES_USER` | Usuário do Postgres | `infofolga` |
| `POSTGRES_PASSWORD` | Senha do Postgres | — |
| `SPRING_DATASOURCE_URL` | JDBC URL usada pela API | `jdbc:postgresql://banco:5432/infofolga_db` |
| `SPRING_DATASOURCE_USERNAME` | Deve ser igual a `POSTGRES_USER` | `infofolga` |
| `SPRING_DATASOURCE_PASSWORD` | Deve ser igual a `POSTGRES_PASSWORD` | — |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Estratégia de schema do Hibernate (padrão `update`) | `update` |
| `JWT_SECRET` | Chave HMAC256 para assinar os tokens | string longa e aleatória |

O schema é criado/atualizado automaticamente pelo Hibernate (`ddl-auto=update`); não há migrations.

---

## Primeiro acesso

Não existe usuário inicial. Crie o primeiro **CEO** direto no banco, com a senha em hash BCrypt:

```sql
INSERT INTO colaboradores (nome, cpf, email, senha, cargo, setor, role, status)
VALUES ('Admin', '12345678900', 'admin@empresa.com',
        '<hash-bcrypt-da-senha>', 'CEO', 'Diretoria', 'CEO', 'ativo');
```

Gere o hash com qualquer ferramenta BCrypt (ex.: `htpasswd -bnBC 10 "" 'minhaSenha123' | tr -d ':\n'`). A partir daí, os demais colaboradores são cadastrados pela API.

> **CPF sempre só com números.** O login remove pontos e traços do CPF antes de buscar o usuário, mas o cadastro grava o valor como veio. Um colaborador cadastrado como `123.456.789-00` não consegue logar.

---

## Autenticação

```http
POST /api/auth/login
Content-Type: application/json

{ "cpf": "123.456.789-00", "senha": "minhaSenha123" }
```

Resposta:

```json
{ "token": "eyJhbGciOi...", "nomeUsuario": "Admin", "role": "CEO" }
```

Envie o token nas demais requisições:

```http
Authorization: Bearer eyJhbGciOi...
```

- O token expira em **2 horas**.
- O `subject` do token é o CPF do colaborador.
- Colaboradores com `status = "inativo"` não conseguem logar.

---

## Perfis de acesso

Os perfis são hierárquicos — cada um herda as permissões do anterior:

| Role | Authorities concedidas |
|---|---|
| `FUNCIONARIO` | `ROLE_FUNCIONARIO` |
| `GERENTE` | `ROLE_GERENTE`, `ROLE_FUNCIONARIO` |
| `CEO` | `ROLE_CEO`, `ROLE_GERENTE`, `ROLE_FUNCIONARIO` |

---

## Endpoints

Todos exigem `Authorization: Bearer <token>`, exceto o login. A coluna **Acesso** indica o perfil mínimo.

### Auth

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/auth/login` | público | Retorna o JWT |

### Colaboradores

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/api/colaboradores/me` | autenticado | Dados do usuário logado |
| `GET` | `/api/colaboradores/me/stats` | autenticado | Estatísticas do usuário logado |
| `GET` | `/api/colaboradores` | GERENTE | Lista todos os colaboradores |
| `POST` | `/api/colaboradores` | CEO | Cadastra colaborador |
| `PUT` | `/api/colaboradores/{id}` | CEO | Atualiza (apenas campos enviados; senha só se não vazia) |
| `DELETE` | `/api/colaboradores/{id}` | CEO | Exclui — falha com 400 se houver solicitações vinculadas |
| `PUT` | `/api/colaboradores/{id}/inativar` | CEO | Bloqueia o acesso (preserva histórico) |
| `PUT` | `/api/colaboradores/{id}/reativar` | CEO | Libera o acesso novamente |
| `PUT` | `/api/colaboradores/{id}/promover` | — | Define role como `GERENTE` |
| `PUT` | `/api/colaboradores/{id}/rebaixar` | — | Define role como `FUNCIONARIO` |

Corpo de cadastro:

```json
{
  "nome": "Maria Silva",
  "cpf": "12345678900",
  "email": "maria@empresa.com",
  "senha": "senhaForte123",
  "cargo": "Desenvolvedora",
  "setor": "TI",
  "foto": "data:image/png;base64,...",
  "role": "FUNCIONARIO"
}
```

`nome`, `cpf`, `email`, `senha` (mín. 8 caracteres) e `role` são obrigatórios. `foto` é uma string livre (tipicamente base64) salva como `TEXT`.

### Solicitações

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/solicitacoes` | autenticado | Cria solicitação (status `PENDENTE`) |
| `GET` | `/api/solicitacoes/minhas` | autenticado | Solicitações do usuário logado |
| `GET` | `/api/solicitacoes/todas` | GERENTE | Todas as solicitações |
| `PUT` | `/api/solicitacoes/{id}/aprovar` | GERENTE | → `APROVADA` |
| `PUT` | `/api/solicitacoes/{id}/rejeitar` | GERENTE | → `REJEITADA` (exige `motivoResposta`) |
| `DELETE` | `/api/solicitacoes/{id}` | dono | → `CANCELADA` (não apaga o registro) |
| `PUT` | `/api/solicitacoes/{id}/usufruir` | dono ou GERENTE | → `USUFRUIDA` |
| `PUT` | `/api/solicitacoes/{id}/invalidar` | dono | Pede estorno → `ESTORNO_PENDENTE` |
| `PUT` | `/api/solicitacoes/{id}/aprovar-estorno` | GERENTE | → `INVALIDADA` |
| `PUT` | `/api/solicitacoes/{id}/rejeitar-estorno` | GERENTE | Volta para `APROVADA` |

Criar:

```json
{ "tipo": "FOLGA", "dataInicio": "2026-10-01", "dataFim": "2026-10-02", "motivo": "Consulta médica" }
```

`tipo`: `FOLGA` ou `FERIAS`. Rejeitar:

```json
{ "motivoResposta": "Período com entrega crítica" }
```

Cada solicitação guarda um **snapshot** de nome, cargo, setor e foto do colaborador no momento da criação (`nomeHistorico`, `cargoHistorico`, …), para que o histórico não mude se o cadastro for editado depois.

### Dashboard

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/api/dashboard` | GERENTE | Indicadores gerais |

```json
{
  "totalColaboradores": 42,
  "totalSolicitacoes": 310,
  "aprovadas30Dias": 18,
  "rejeitadas30Dias": 3,
  "pendentes30Dias": 5,
  "naoUsufruidas30Dias": 1,
  "folgasHoje": 2,
  "proximasFolgas": 4
}
```

Os contadores "30 dias" consideram a data da última atualização da solicitação. `folgasHoje` conta `APROVADA`/`USUFRUIDA` cujo período inclui hoje; `proximasFolgas` conta `APROVADA` que começam nos próximos 7 dias.

O resultado fica em cache em memória e é invalidado a cada alteração em solicitações.

---

## Ciclo de vida de uma solicitação

```
                ┌──────────► REJEITADA
                │
PENDENTE ───────┼──────────► CANCELADA
                │
                └──► APROVADA ──────────► USUFRUIDA
                        │  ▲
          (invalidar)   ▼  │ (rejeitar-estorno)
                  ESTORNO_PENDENTE
                        │
                        ▼ (aprovar-estorno)
                    INVALIDADA
```

> As transições não são validadas no servidor: qualquer endpoint de mudança de status aceita a solicitação em qualquer estado. O fluxo acima é o esperado pelo front-end.

---

## Erros

Os erros de negócio e validação seguem o formato:

```json
{ "erro": "Mensagem legível" }
```

| Status | Quando |
|---|---|
| `400` | Validação do corpo falhou, ou exclusão de colaborador com histórico |
| `401` | Rota protegida sem token válido |
| `403` | Login inválido, ou perfil sem permissão |
| `404` | Recurso não encontrado |
| `500` | Erro inesperado |

---

## Estrutura do projeto

```
src/main/java/com/infoway/infofolga/
├── controller/    # Endpoints REST
├── dto/           # Records de entrada e saída
├── exception/     # GlobalExceptionHandler
├── model/         # Entidades JPA (Colaborador, Solicitacao) e enums
├── repository/    # Spring Data JPA
├── security/      # SecurityFilterChain e filtro JWT
├── service/       # Regras de negócio, token e cache
└── util/          # CpfUtils
```

Arquivos auxiliares:

- `teste-carga.js` — script [k6](https://k6.io) de teste de carga (login + leituras). Rode com `k6 run src/main/java/com/infoway/infofolga/teste-carga.js`; ajuste as credenciais no script antes.
- `relatorio-carga-final.html` — relatório gerado pelo último teste de carga.

---

## Testes

```bash
./mvnw test
```

Hoje existe apenas o teste de carregamento do contexto (`contextLoads`), que precisa de um banco acessível e das variáveis de ambiente configuradas.
