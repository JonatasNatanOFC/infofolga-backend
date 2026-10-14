# Segurança e isolamento de dados

O que protege os dados no InfoFolga, onde isso está no código e como é verificado.

## Isolamento entre empresas (multiempresa)

Várias empresas usam o mesmo banco. Cada colaborador, solicitação, regra, feriado, batida, apuração e ajuste tem `empresa_id`.

- As entidades usam `@TenantId` do Hibernate. `EmpresaAtualResolver` informa a empresa do usuário logado, e o Hibernate acrescenta `empresa_id = ?` em **toda consulta** e preenche o campo em **todo insert**. Nenhum repositório precisa lembrar de filtrar.
- Sem usuário autenticado (login e validação do token), as consultas não são filtradas. Isso só é seguro porque nesses pontos a busca é por CPF, que é único no sistema inteiro.
- Na importação do AFD, um CPF de outra empresa é tratado como "não encontrado": o RH de uma empresa não consegue gravar batidas em colaborador de outra.

**Verificação:** `IsolamentoEntreEmpresasTest` roda contra um PostgreSQL real (Testcontainers) e confere que um usuário não lista, não lê e não altera dados de outra empresa. `ImportacaoPontoTest`, `ExtratoTest` e `ConfiguracaoPontuacaoTest` repetem a checagem para os dados de pontos.

## Autenticação

- Login por CPF e senha (`POST /api/auth/login`). Senhas com BCrypt custo 10.
- Token JWT HMAC-256 com validade de **7 dias**. O app renova o token ao abrir e ao voltar para primeiro plano, no máximo uma vez por hora (`POST /api/auth/renovar`). Só é deslogado quem fica 7 dias sem usar.
- A API se recusa a subir se `JWT_SECRET` tiver menos de 32 bytes.
- **Inativar um colaborador corta o acesso na hora**, mesmo com token válido: o `SecurityFilter` recarrega o colaborador a cada requisição e descarta quem está inativo.
- Sessão sem estado (`STATELESS`), sem cookie, então CSRF fica desligado.

**Verificação:** `TokenServiceTest`.

## Autorização

Cada endpoint declara o perfil exigido com `@PreAuthorize`. As permissões ficam em `Colaborador.getAuthorities()`:

| Perfil | Authorities | Pode |
|---|---|---|
| `FUNCIONARIO` | `ROLE_FUNCIONARIO` | Pedir folga e férias, ver o próprio saldo |
| `GERENTE` | `ROLE_GERENTE`, `ROLE_FUNCIONARIO` | Aprovar e rejeitar pedidos, ver colaboradores e dashboard |
| `RH` | `ROLE_RH`, `ROLE_FUNCIONARIO` | Pontuação, feriados, importação do ponto, saldo da equipe, ajustes. **Não aprova folgas** |
| `CEO` | `ROLE_CEO`, `ROLE_GERENTE`, `ROLE_FUNCIONARIO` | Tudo do gerente, mais cadastro de colaboradores, pontuação e ponto |

Regras que não dependem só do perfil, verificadas nos serviços:

- ninguém avalia a própria solicitação;
- ninguém lança ajuste no próprio saldo;
- só o dono cancela, pede estorno ou marca como usufruída a própria solicitação (o gerente também pode marcar como usufruída);
- o perfil `CEO` não pode ser promovido nem rebaixado pela API.

**Verificação:** `RegrasDeNegocioTest`, `PerfilRhTest`.

## Proteções na borda

| Proteção | Onde |
|---|---|
| Limite de 100 requisições por minuto por IP (`429`) | `RateLimitingInterceptor` |
| HSTS de 1 ano, `X-Frame-Options: DENY`, `X-Content-Type-Options` | `SecurityConfigurations` |
| Erros sem stack trace nem mensagem interna | `application.properties` (`server.error.include-*=never`) |
| Upload limitado a 20 MB; foto limitada a 2 MB e a `jpeg`, `png` ou `webp` | `application.properties`, `FotoUtils` |
| API publicada só em `127.0.0.1`, atrás do nginx com HTTPS | `docker-compose.prod.yml`, `API_BIND` |
| Banco publicado só em `127.0.0.1` | `docker-compose.prod.yml` |

O rate limit lê o IP do primeiro valor de `X-Forwarded-For`. Por isso o nginx **sobrescreve** esse cabeçalho com `$remote_addr` (ver `DEPLOYMENT.md`); sem isso, o cliente poderia trocar de IP a cada requisição.

Em produção o app web e a API são servidos pelo mesmo domínio (o nginx entrega o app e repassa `/api` para a API), então o navegador não faz requisição entre origens. A lista de CORS em `WebConfig` só cobre o desenvolvimento local.

## Dados pessoais

- CPF só com números. Na importação, CPFs não encontrados voltam mascarados (`529.***.***-25`).
- Cada solicitação guarda uma cópia de nome, cargo, setor e foto do momento do pedido, então o histórico não muda se o cadastro for editado depois.
- Inativar preserva o histórico; excluir é bloqueado para quem tem solicitações.
- Segredos (`.env`, `.env.production`) ficam fora do Git. O app não guarda nenhum segredo: só a URL da API.
- Backup diário do banco para o S3 (`scripts/backup-infofolga.sh`).

## Pontos de atenção

| Item | Risco | Situação |
|---|---|---|
| Rate limit em memória | Não é compartilhado se houver mais de uma instância da API | Hoje roda uma instância só |
| Token não é revogável antes de expirar | Um token roubado vale até 7 dias | Mitigado: inativar o colaborador corta o acesso na hora |
| Domínio provisório (`sslip.io`) | Depende de um serviço de DNS de terceiros | Trocar por domínio próprio antes de abrir para clientes |
