package com.infoway.infofolga.pontuacao;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Dias de exemplo apurados com as regras informadas, para o RH ver o efeito de uma mudança antes de salvar.
 * Usa o mesmo cálculo da apuração real, então o que aparece aqui é o que vai acontecer.
 */
public final class ExemplosPontuacao {

    public record Exemplo(String titulo, ResultadoDia resultado) {
    }

    // Uma semana qualquer: a situação de cada dia é informada, então feriados reais não interferem.
    private static final LocalDate SEGUNDA = LocalDate.of(2026, 10, 19);
    private static final LocalDate DOMINGO = SEGUNDA.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
    private static final LocalTime ENTRADA = LocalTime.of(8, 0);
    private static final LocalTime ALMOCO = LocalTime.of(12, 0);

    private ExemplosPontuacao() {
    }

    public static List<Exemplo> com(RegrasPontuacao regras) {
        ApuracaoDiaria apuracao = new ApuracaoDiaria(regras);
        Duration jornada = regras.jornadaDe(SEGUNDA);
        List<Exemplo> exemplos = new ArrayList<>();

        exemplos.add(exemplo(apuracao, "Segunda: saiu 1h depois",
                turno(SEGUNDA, ENTRADA, jornada.plusHours(1)), SituacaoDia.NORMAL));
        exemplos.add(exemplo(apuracao, "Segunda: chegou 8 min atrasado",
                turno(SEGUNDA, ENTRADA.plusMinutes(8), jornada.minusMinutes(8)), SituacaoDia.NORMAL));
        exemplos.add(exemplo(apuracao, "Segunda: chegou 30 min atrasado",
                turno(SEGUNDA, ENTRADA.plusMinutes(30), jornada.minusMinutes(30)), SituacaoDia.NORMAL));
        exemplos.add(exemplo(apuracao, "Domingo: 4h de plantão",
                turno(DOMINGO, ENTRADA, Duration.ofHours(4)), SituacaoDia.NORMAL));
        exemplos.add(exemplo(apuracao, "Feriado: 4h de plantão",
                turno(SEGUNDA, ENTRADA, Duration.ofHours(4)), SituacaoDia.FERIADO));
        exemplos.add(exemplo(apuracao, "Domingo: chamado das 22h às 2h",
                List.of(DOMINGO.atTime(22, 0), DOMINGO.plusDays(1).atTime(2, 0)), SituacaoDia.NORMAL));
        exemplos.add(exemplo(apuracao, "Segunda: folga do banco de horas", List.of(), SituacaoDia.FOLGA_COM_DESCONTO));
        exemplos.add(exemplo(apuracao, "Segunda: falta sem justificativa", List.of(), SituacaoDia.FALTA_INJUSTIFICADA));
        return exemplos;
    }

    private static Exemplo exemplo(ApuracaoDiaria apuracao, String titulo, List<LocalDateTime> batidas,
                                   SituacaoDia situacao) {
        LocalDate data = batidas.isEmpty() ? SEGUNDA : batidas.get(0).toLocalDate();
        return new Exemplo(titulo, apuracao.apurar(new DiaTrabalho(data, batidas, situacao)));
    }

    /** Batidas de quem entra no horário dado e trabalha o tempo dado, com 1h de almoço ao meio-dia se passar dele. */
    static List<LocalDateTime> turno(LocalDate dia, LocalTime entrada, Duration trabalho) {
        if (trabalho.isNegative() || trabalho.isZero()) {
            return List.of();
        }
        LocalDateTime inicio = dia.atTime(entrada);
        LocalDateTime fim = inicio.plus(trabalho);
        LocalDateTime almoco = dia.atTime(ALMOCO);
        if (!fim.isAfter(almoco)) {
            return List.of(inicio, fim);
        }
        return List.of(inicio, almoco, almoco.plusHours(1), fim.plusHours(1));
    }
}
