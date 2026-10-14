# Importação do ponto (AFD)

O InfoFolga não registra ponto. Ele lê as batidas do relógio que a empresa já usa e calcula o banco de horas a partir delas.

## De onde vêm as batidas

Hoje, do **AFD** (Arquivo-Fonte de Dados) que todo relógio de ponto homologado exporta (Portaria MTP 671/2021). O CEO ou o RH envia o arquivo pela tela **Importar ponto**, que chama:

```http
POST /api/pontos/importar-afd
Content-Type: multipart/form-data   (campo "arquivo", até 20 MB)
```

Toda entrada passa por `ImportacaoPontoService.registrar(...)`. Um futuro cliente da API do Faceponto (ou de outro fornecedor) vai chamar o mesmo método, então o resto do fluxo não muda com a fonte.

## Leiaute lido

`LeitorAfd` lê só os registros de marcação: tipo `3` (REP-C e REP-A) e tipo `7` (REP-P).

| Posição | Conteúdo |
|---|---|
| 1–9 | NSR (número sequencial do registro) |
| 10 | Tipo do registro (`3` ou `7`) |
| 11–34 | Data e hora, `AAAA-MM-ddThh:mm:00-0300` |
| 35–46 | CPF do empregado |

- Cabeçalho, dados da empresa, ajustes de relógio, trailer e assinatura são ignorados.
- A mesma batida repetida no arquivo conta uma vez.
- **Formato antigo** (Portaria 1510, que identifica por PIS) é detectado e recusado com uma mensagem pedindo o AFD da Portaria 671.
- O arquivo é lido como ISO-8859-1, que aceita qualquer byte sem falhar.

## O que acontece na importação

1. As marcações são agrupadas por CPF.
2. O CPF é procurado **só na empresa de quem importa** (filtro `@TenantId`). CPF de outra empresa conta como "não encontrado".
3. Só as batidas novas são gravadas na tabela `batidas`. Reimportar o mesmo arquivo não duplica nada.
4. Os dias de quem recebeu batidas são recalculados (`ApuracaoService.reprocessar`), até ontem. Hoje fica para a próxima importação.

A resposta resume o que foi feito:

```json
{
  "linhas": 1204,
  "marcacoes": 1180,
  "novas": 1180,
  "formatoAntigo": 0,
  "cpfsNaoEncontrados": ["529.***.***-25"],
  "inicio": "2026-10-01",
  "fim": "2026-10-31",
  "diasCalculados": 506
}
```

Os CPFs não encontrados voltam mascarados: o RH reconhece a pessoa sem o número inteiro aparecer na tela.

## Preciso importar todos os meses?

Não. As batidas ficam guardadas no InfoFolga, então basta importar o período novo. O AFD costuma trazer o histórico inteiro do relógio, e as batidas que já existem são ignoradas. Para começar a usar, importe a partir da data em que a pontuação da empresa passa a valer (`vigente_desde`) e lance o saldo anterior como ajuste manual.

## Corrigir uma batida

A correção é feita **no relógio ou no sistema de ponto**, não no InfoFolga. Na importação seguinte o dia é recalculado sozinho. Diferenças que não vêm do ponto (saldo inicial, acordo com o colaborador) entram como ajuste manual, com justificativa.

## Batidas brutas são a fonte

O InfoFolga guarda as batidas brutas, não só o resultado. Assim:

- mudar as regras e recalcular um período não depende do sistema de ponto de origem;
- se a empresa trocar de fornecedor de ponto, o histórico continua no InfoFolga e as batidas novas entram pela mesma porta (`registrar`).
