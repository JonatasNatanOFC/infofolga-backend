# Banco de horas (pontos)

Como o InfoFolga transforma as batidas do ponto em saldo. **1 ponto = 1 hora** acima ou abaixo da jornada prevista.

Todo o cálculo fica no pacote `pontuacao/`, sem acesso a banco nem ao Spring: recebe batidas e regras, devolve o resultado do dia. Por isso ele é testado isoladamente (`ApuracaoDiariaTest`, `AgrupadorDeBatidasTest`, `FeriadosNacionaisTest`, `ExemplosPontuacaoTest`).

| Classe | Papel |
|---|---|
| `RegrasPontuacao` | Regras de uma empresa: jornada por dia da semana, tolerância, pesos, hora noturna, falta |
| `AgrupadorDeBatidas` | Separa as batidas por dia de trabalho (turno que passa da meia-noite fica no dia em que começou) |
| `ApuracaoDiaria` | Calcula os pontos de um dia |
| `SituacaoDia` | O que aconteceu no dia: normal, feriado, folga, férias, falta |
| `FeriadosNacionais` | Feriados nacionais fixos e móveis (Páscoa, Carnaval, Corpus Christi) de qualquer ano |
| `PoliticaSemSaldo` | O que fazer quando pedem folga sem saldo |

## Quem tem banco de horas

| Perfil | Acumula pontos? |
|---|---|
| `FUNCIONARIO` | Sim |
| `RH` | Só se a empresa marcar "RH participa" (na Infoway o RH é terceirizado, então não) |
| `GERENTE`, `CEO` | Não |

Regra em `Colaborador.temBancoDeHoras(rhParticipa)`. Quem não tem banco de horas não é apurado, não vê a tela de saldo (`GET /api/pontos/meus` responde `403`) e as folgas dele não descontam nada.

## Regras configuráveis por empresa

Ficam na tabela `configuracao_pontuacao` e são editadas na tela **Pontuação e feriados** (CEO e RH).

| Campo | Padrão | O que faz |
|---|---|---|
| `jornada_seg` … `jornada_dom` | 8h seg–sex, 4h sáb, 0 dom | Horas previstas em cada dia da semana (em minutos no banco) |
| `tolerancia_min` | 10 | Diferença do dia que não conta (CLT art. 58 §1º). Passou do limite, conta a diferença **inteira** (Súmula 366 do TST) |
| `peso_domingo` | 1 | Multiplica as horas **a mais** trabalhadas no domingo |
| `peso_feriado` | 1 | Multiplica as horas **a mais** trabalhadas em feriado |
| `fator_hora_noturna` | 8/7 | Hora entre 22h e 5h vale 52min30s (CLT art. 73) |
| `compensar_falta` | não | Se a falta sem justificativa desconta do saldo (senão é só descontada em folha) |
| `politica_sem_saldo` | `AVISAR` | `PERMITIR`, `AVISAR` ou `BLOQUEAR` folga do banco sem saldo |
| `rh_tem_banco_de_horas` | não | Se o RH acumula pontos |
| `vigente_desde` | — | Data a partir da qual estas regras valem |

### Regras versionadas

Salvar regras novas **não reescreve o passado**: cria uma linha nova com `vigente_desde`. Cada dia é calculado com a versão em vigor naquela data (`RegrasPontuacaoService.regrasEm`). Dias anteriores à primeira versão não são apurados: a pontuação de uma empresa só começa quando ela define a data de início.

Antes de salvar, a tela mostra uma simulação (`POST /api/pontuacao/simular`) com dias de exemplo calculados pelas regras novas, para o gestor ver o efeito antes de confirmar.

## Como um dia é calculado

1. **Situação do dia.** Folga aprovada, usufruída ou com estorno pendente vira `FOLGA_COM_DESCONTO` ou `FOLGA_SEM_DESCONTO`, conforme o gerente decidiu ao aprovar. Férias viram `FERIAS`. Senão, o dia é `FERIADO` (nacional ou cadastrado pela empresa) ou `NORMAL`.
2. **Previsto.** Jornada do dia da semana para `NORMAL`, `FOLGA_COM_DESCONTO` e `FALTA_INJUSTIFICADA`; zero para feriado, folga sem desconto, férias e falta justificada.
3. **Trabalhado.** Soma dos pares entrada/saída. O trecho entre 22h e 5h é multiplicado pelo fator noturno. Se o turno cobre a noite inteira e segue depois das 5h, as horas seguintes também contam como noturnas (Súmula 60 do TST).
4. **Diferença** = trabalhado − previsto. Se ficar dentro da tolerância, vira zero.
5. **Peso.** Só multiplica diferença positiva. Desconto vale sempre 1 para 1.
6. **Pontos** = diferença × peso ÷ 3600, arredondado em duas casas (`HALF_UP`).

Exemplos (regras padrão, tolerância 10 min):

| Dia | Batidas | Pontos |
|---|---|---|
| Segunda | 08:00–12:00, 13:00–18:00 | +1,00 |
| Segunda | 08:30–12:00, 13:00–17:00 | −0,50 |
| Segunda | 08:05–12:00, 13:00–17:00 | 0,00 (5 min, dentro da tolerância) |
| Domingo | 08:00–12:00 | +4,00 (× peso de domingo) |
| Domingo | 22:00–02:00 | +4,57 (4h noturnas × 8/7) |
| Folga do banco numa segunda | — | −8,00 |

### Dias que ficam fora

- **Batida faltando** (número ímpar de marcações): o dia fica com 0 pontos e marcado como inconsistente até o RH corrigir no relógio. É recalculado sozinho na próxima importação.
- **Dia sem batida e sem folga** não é tratado como falta: o AFD não traz atestados nem escalas, então o sistema não tem como saber se foi falta, atestado ou escala. Ver [limitações](#limitações-conhecidas).
- **Hoje** não é apurado: o dia ainda está acontecendo.

## Saldo

```
saldo = soma dos pontos apurados (apuracao_dia) + soma dos ajustes manuais (ajustes_saldo)
```

- **Recalcular é seguro.** O resultado de cada dia é substituído, nunca somado. Importar o mesmo arquivo duas vezes não duplica nada.
- **Ajuste manual** (`POST /api/pontos/{id}/ajustes`): CEO ou RH lança pontos com justificativa obrigatória, por exemplo o saldo inicial vindo do sistema antigo. Ninguém lança ajuste no próprio saldo. O ajuste guarda quem lançou e aparece no extrato.

## Folga sem saldo

Quando alguém pede folga do banco de horas, o sistema calcula:

- **saldo disponível** = saldo atual − folgas do banco já pedidas (pendentes ou aprovadas) cujos dias ainda não foram apurados;
- **pontos da folga** = jornada prevista dos dias pedidos (feriado não conta).

Se não houver saldo suficiente, vale a política da empresa:

| Política | Efeito |
|---|---|
| `PERMITIR` | O pedido passa e o saldo pode ficar negativo |
| `AVISAR` | O pedido passa; o gerente vê "Saldo insuficiente no pedido" antes de aprovar |
| `BLOQUEAR` | O app não deixa enviar e a API responde `409` |

O saldo do momento do pedido fica gravado na solicitação (`saldo_no_pedido`, `pontos_da_folga`), então o gerente decide com o número que valia quando o pedido foi feito. O app mostra a prévia antes do envio (`GET /api/pontos/previa-folga`).

## Conferência com o Faceponto

O cálculo foi comparado com o espelho de ponto do Faceponto (sistema usado pela Infoway) de um mês inteiro (setembro de 2026):

- os 26 dias trabalhados bateram com diferença de no máximo 1 minuto;
- o total do mês ficou −0:39 no InfoFolga contra −0:35 no Faceponto. A diferença vem dos segundos: o Faceponto arredonda cada dia ao minuto.

Para reproduzir o Faceponto da Infoway, a configuração é: **tolerância 0**, pesos de domingo e feriado **1** e hora noturna reduzida **ligada**. O saldo anterior à adoção entra como um ajuste manual.

## Limitações conhecidas

Estão marcadas no código com `shortcut:` e listadas aqui para quem for auditar:

| Limitação | Onde | Quando resolver |
|---|---|---|
| A tolerância aplica só o limite diário (10 min); a CLT também limita 5 min por marcação, o que exige o horário de cada batida na escala | `ApuracaoDiaria` | Quando a escala vier da API do Faceponto |
| Dia sem batida não vira falta | `ApuracaoService` | Quando a fonte de dados informar faltas e atestados |
| O fuso do AFD (`-0300`) é descartado; vale o horário do relógio | `LeitorAfd` | Se uma empresa tiver filial em outro fuso |
| Turno aberto há mais de 14h não recebe a próxima batida (é tratado como esquecimento) | `AgrupadorDeBatidas` | Se houver plantões maiores que 14h |
