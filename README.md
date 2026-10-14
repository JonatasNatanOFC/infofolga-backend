# InfoFolga API

API REST para gestão de **folgas, férias e banco de horas**. O colaborador pede folga pelo app, o gestor aprova, e o saldo de horas é calculado sozinho a partir do ponto eletrônico da empresa. Atende várias empresas no mesmo servidor, com os dados isolados entre elas.

**Stack:** Java 21 · Spring Boot 3.5 · Spring Security + JWT (Auth0 `java-jwt`) · Spring Data JPA / Hibernate · PostgreSQL 16 · Testcontainers · Docker

App (React Native + Expo): repositório `InfoFolga-App`.

## Documentação

| Documento | Conteúdo |
|---|---|
| Este README | Como rodar, perfis, endpoints, ciclo de vida das solicitações |
| [docs/BANCO_DE_HORAS.md](docs/BANCO_DE_HORAS.md) | Como as batidas viram pontos: jornada, tolerância, hora noturna, pesos, folga sem saldo, conferência com o Faceponto |
| [docs/IMPORTACAO_PONTO.md](docs/IMPORTACAO_PONTO.md) | Leitura do AFD (Portaria 671), importação e recálculo |
| [docs/SEGURANCA.md](docs/SEGURANCA.md) | Isolamento entre empresas, autenticação, autorização, proteções e pontos de atenção |
| [database/README.md](database/README.md) | Migrations V1–V7, banco do zero, criação de empresa |
| [DEPLOYMENT.md](DEPLOYMENT.md) | Servidor, deploy da API e do app web, backup, restauração, rollback |

---

## Como rodar

### Docker Compose (API + banco)

```bash
# crie o .env na raiz (veja "Variáveis de ambiente")
docker compose up --build
```

- API: `http://localhost:8080`
- PostgreSQL: `localhost:5441` (5432 dentro da rede do compose, host `banco`)

### API local, banco no Docker

```bash
docker compose up -d banco
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5441/infofolga_db
export SPRING_DATASOURCE_USERNAME=...
export SPRING_DATASOURCE_PASSWORD=...
export JWT_SECRET=...            # pelo menos 32 bytes: openssl rand -base64 32
./mvnw spring-boot:run
```

O Spring Boot **não** lê o `.env` sozinho: fora do Docker as variáveis precisam estar no ambiente do processo. Para logs detalhados de segurança e web, use `-Dspring-boot.run.profiles=debug`.

O banco precisa ter as migrations aplicadas ([database/README.md](database/README.md)). Em desenvolvimento dá para usar `SPRING_JPA_HIBERNATE_DDL_AUTO=update`, nunca em produção.

## Variáveis de ambiente

O `.env` e o `.env.production` estão no `.gitignore`. **Nunca faça commit deles.**

| Variável | Uso | Exemplo |
|---|---|---|
| `PORT` | Porta HTTP da API (padrão `8080`) | `8080` |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Banco criado pelo container | `infofolga_db`, `infofolga` |
| `SPRING_DATASOURCE_URL` | JDBC URL usada pela API | `jdbc:postgresql://banco:5432/infofolga_db` |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Iguais a `POSTGRES_USER` e `POSTGRES_PASSWORD` | — |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Padrão `validate`: a API não sobe se o schema divergir das entidades | `validate` |
| `JWT_SECRET` | Chave HMAC-256 dos tokens. A API não sobe com menos de 32 bytes | `openssl rand -base64 32` |
| `API_BIND` | Interface onde a 8080 é publicada. Em produção, `127.0.0.1` atrás do nginx | `127.0.0.1` |
| `SPRING_PROFILES_ACTIVE` | `prod` em produção (logs enxutos, mais conexões) | `prod` |

## Primeiro acesso

Não existe usuário inicial. A empresa e o primeiro CEO são criados direto no banco ([database/README.md](database/README.md#banco-novo-do-zero)). Depois disso o CEO cadastra os demais pelo app.

CPF sempre só com números.

---

## Perfis de acesso

| Perfil | Authorities | Papel | Banco de horas |
|---|---|---|---|
| `FUNCIONARIO` | `ROLE_FUNCIONARIO` | Pede folgas e férias, vê o próprio saldo | Sim |
| `GERENTE` | `ROLE_GERENTE`, `ROLE_FUNCIONARIO` | Aprova e rejeita pedidos | Não |
| `RH` | `ROLE_RH`, `ROLE_FUNCIONARIO` | Pontuação, feriados, importação do ponto, saldos da equipe. Não aprova folgas | Se a empresa configurar |
| `CEO` | `ROLE_CEO`, `ROLE_GERENTE`, `ROLE_FUNCIONARIO` | Tudo do gerente, mais cadastro, pontuação e ponto | Não |

## Autenticação

```http
POST /api/auth/login
Content-Type: application/json

{ "cpf": "123.456.789-09", "senha": "minhaSenha123" }
```

```json
{ "token": "eyJhbGciOi...", "nomeUsuario": "Maria", "role": "FUNCIONARIO" }
```

Nas demais requisições: `Authorization: Bearer <token>`.

- O token vale **7 dias**. `POST /api/auth/renovar` devolve um token novo; o app chama ao abrir, então só desloga quem fica 7 dias sem usar.
- Colaborador inativado perde o acesso na hora, mesmo com token válido.
- O login aceita CPF com ou sem pontuação.

---

## Endpoints

Todos exigem token, exceto o login. **Acesso** é o perfil mínimo (`GERENTE` inclui o CEO).

### Auth e empresa

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/auth/login` | público | Retorna o JWT |
| `POST` | `/api/auth/renovar` | autenticado | Retorna um JWT novo |
| `GET` | `/api/empresa` | autenticado | Nome e logos da empresa do usuário |

### Colaboradores

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/api/colaboradores/me` | autenticado | Dados do usuário logado (inclui `temBancoDeHoras`) |
| `PUT` | `/api/colaboradores/me` | autenticado | Atualiza o próprio perfil |
| `GET` | `/api/colaboradores` | GERENTE | Lista os colaboradores |
| `GET` | `/api/colaboradores/{id}` | GERENTE | Detalhes de um colaborador |
| `GET` | `/api/colaboradores/{id}/foto` | autenticado | Foto (binário, com cache) |
| `POST` | `/api/colaboradores` | CEO | Cadastra |
| `PUT` | `/api/colaboradores/{id}` | CEO | Atualiza só os campos enviados; senha só se não vazia |
| `DELETE` | `/api/colaboradores/{id}` | CEO | Exclui; `400` se houver solicitações (use inativar) |
| `PUT` | `/api/colaboradores/{id}/inativar` | CEO | Bloqueia o acesso, preserva o histórico |
| `PUT` | `/api/colaboradores/{id}/reativar` | CEO | Libera o acesso |
| `PUT` | `/api/colaboradores/{id}/promover` | CEO | Vira `GERENTE` (não vale para CEO) |
| `PUT` | `/api/colaboradores/{id}/rebaixar` | CEO | Vira `FUNCIONARIO` (não vale para CEO) |

Cadastro: `nome`, `cpf`, `email`, `senha` (mín. 8 caracteres) e `role` obrigatórios; `cargo`, `setor` e `foto` opcionais. A foto é um data URI base64 (`jpeg`, `png` ou `webp`, até 2 MB).

### Solicitações

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/solicitacoes` | autenticado | Cria (status `PENDENTE`) |
| `GET` | `/api/solicitacoes/minhas` | autenticado | Do usuário logado |
| `GET` | `/api/solicitacoes/todas?status=&tipo=` | GERENTE ou RH | Todas, com filtro |
| `GET` | `/api/solicitacoes/gestao?status=&tipo=&pagina=0&tamanho=20` | GERENTE | Paginado |
| `GET` | `/api/solicitacoes/{id}/foto` | autenticado | Foto do colaborador no momento do pedido |
| `PUT` | `/api/solicitacoes/{id}/aprovar` | GERENTE | → `APROVADA`. Corpo opcional `{ "descontarSaldo": true }` |
| `PUT` | `/api/solicitacoes/{id}/rejeitar` | GERENTE | → `REJEITADA`. Exige `{ "motivoResposta": "..." }` |
| `DELETE` | `/api/solicitacoes/{id}` | dono | → `CANCELADA` (o registro fica) |
| `PUT` | `/api/solicitacoes/{id}/usufruir` | dono ou GERENTE | → `USUFRUIDA` |
| `PUT` | `/api/solicitacoes/{id}/invalidar` | dono | Pede estorno → `ESTORNO_PENDENTE` |
| `PUT` | `/api/solicitacoes/{id}/aprovar-estorno` | GERENTE | → `INVALIDADA` |
| `PUT` | `/api/solicitacoes/{id}/rejeitar-estorno` | GERENTE | Volta para `APROVADA` |

```json
{ "tipo": "FOLGA", "dataInicio": "2026-11-03", "dataFim": "2026-11-03", "motivoFolga": "BANCO_DE_HORAS", "motivo": "Consulta" }
```

`tipo`: `FOLGA` ou `FERIAS`. `motivoFolga`: `BANCO_DE_HORAS` (padrão), `ANIVERSARIO` ou `OUTRO`. O motivo sugere se a folga desconta do saldo; quem decide é o gestor, ao aprovar.

### Pontos (banco de horas)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/api/pontos/meus?mes=2026-10` | quem tem banco de horas | Saldo e extrato do mês |
| `GET` | `/api/pontos/previa-folga?inicio=&fim=` | autenticado | Saldo disponível, pontos da folga e política da empresa |
| `GET` | `/api/pontos/equipe` | CEO ou RH | Saldo de cada colaborador |
| `GET` | `/api/pontos/{colaboradorId}?mes=` | CEO ou RH | Extrato de um colaborador |
| `POST` | `/api/pontos/{colaboradorId}/ajustes` | CEO ou RH | Ajuste manual `{ "data", "pontos", "justificativa" }` |
| `DELETE` | `/api/pontos/ajustes/{id}` | CEO ou RH | Remove um ajuste |
| `POST` | `/api/pontos/importar-afd` | CEO ou RH | Importa o AFD (multipart, campo `arquivo`, até 20 MB) |

### Pontuação e feriados

Todos exigem CEO ou RH.

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/pontuacao/configuracao` | Histórico das regras (cada versão com `vigenteDesde`) |
| `POST` | `/api/pontuacao/configuracao` | Salva uma versão nova das regras |
| `POST` | `/api/pontuacao/simular` | Calcula dias de exemplo com as regras enviadas, sem salvar |
| `GET` | `/api/pontuacao/feriados?ano=2026` | Feriados nacionais e da empresa no ano |
| `POST` | `/api/pontuacao/feriados` | Cadastra feriado `{ "data", "descricao", "anual" }` |
| `DELETE` | `/api/pontuacao/feriados/{id}` | Remove feriado da empresa |

### Dashboard

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/api/dashboard` | GERENTE | Totais da equipe (em cache por até 5 min, limpo a cada mudança em solicitações) |

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

Regras validadas no servidor (e repetidas no app para avisar antes):

| Regra | Folga | Férias |
|---|---|---|
| Antecedência mínima | 3 dias | 15 dias |
| Data máxima de início | 30 dias à frente | 2 anos à frente |
| Duração | 1 dia | livre |

- Não pode haver outra solicitação sua (pendente, aprovada, usufruída ou com estorno pendente) no mesmo período.
- Ninguém avalia a própria solicitação.
- Operação fora do status esperado responde `409`.
- Folga do banco de horas sem saldo segue a política da empresa ([docs/BANCO_DE_HORAS.md](docs/BANCO_DE_HORAS.md#folga-sem-saldo)).
- Cada solicitação guarda nome, cargo, setor e foto do colaborador no momento do pedido, então o histórico não muda se o cadastro for editado.

## Erros

```json
{ "erro": "Mensagem legível", "message": "Mensagem legível" }
```

| Status | Quando |
|---|---|
| `400` | Validação falhou, arquivo inválido, exclusão de colaborador com histórico |
| `401` | Login inválido, ou sem token válido |
| `403` | Perfil sem permissão, ou ação sobre algo que não é seu |
| `404` | Não encontrado (inclui dados de outra empresa) |
| `409` | Duplicidade, status inválido, período ocupado, folga bloqueada por falta de saldo |
| `429` | Mais de 100 requisições por minuto do mesmo IP |

---

## Estrutura

```
src/main/java/com/infoway/infofolga/
├── config/        # EmpresaAtualResolver (multiempresa), CORS
├── controller/    # Endpoints REST
├── dto/           # Records de entrada e saída, com validação
├── exception/     # GlobalExceptionHandler
├── filter/        # Rate limit
├── model/         # Entidades JPA e enums
├── ponto/         # Leitura do AFD
├── pontuacao/     # Cálculo do banco de horas (sem Spring, sem banco)
├── repository/    # Spring Data JPA
├── security/      # SecurityFilterChain e filtro JWT
├── service/       # Regras de negócio
└── util/          # CPF, fotos
database/          # Migrations SQL (V1–V7)
docs/              # Documentação de domínio e segurança
scripts/           # Backup do banco
load-tests/        # Teste de carga k6
```

## Testes

```bash
./mvnw test
```

Precisa do Docker rodando: os testes de integração sobem um PostgreSQL de verdade com Testcontainers.

| Área | Testes |
|---|---|
| Cálculo do banco de horas | `ApuracaoDiariaTest`, `AgrupadorDeBatidasTest`, `FeriadosNacionaisTest`, `ExemplosPontuacaoTest` |
| Leitura do AFD | `LeitorAfdTest` |
| Isolamento entre empresas | `IsolamentoEntreEmpresasTest`, `ImportacaoPontoTest`, `ExtratoTest`, `ConfiguracaoPontuacaoTest` |
| Regras de solicitação e perfis | `RegrasDeNegocioTest`, `PerfilRhTest` |
| Token | `TokenServiceTest` |

Teste de carga (k6), com credenciais pelo ambiente:

```bash
k6 run -e GERENTE_CPF=... -e GERENTE_SENHA=... -e FUNC_CPF=... -e FUNC_SENHA=... load-tests/teste-carga.js
```
