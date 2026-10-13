package com.infoway.infofolga.pontuacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Regras de pontuação de uma empresa. 1 ponto = 1 hora acima da jornada; atraso e saída antecipada descontam.
 *
 * @param jornada               horas previstas em cada dia da semana (dia ausente = 0)
 * @param toleranciaDiaria      diferença do dia que não conta, nem para mais nem para menos (CLT, art. 58, §1º)
 * @param pesoDomingo           multiplica as horas a mais trabalhadas no domingo
 * @param pesoFeriado           multiplica as horas a mais trabalhadas em feriado
 * @param fatorHoraNoturna      quanto vale cada hora real entre 22h e 5h (CLT: hora de 52min30s, ou seja 8/7)
 * @param compensarFaltaNoBanco se a falta sem justificativa desconta a jornada do saldo, em vez de só da folha
 */
public record RegrasPontuacao(
        Map<DayOfWeek, Duration> jornada,
        Duration toleranciaDiaria,
        BigDecimal pesoDomingo,
        BigDecimal pesoFeriado,
        BigDecimal fatorHoraNoturna,
        boolean compensarFaltaNoBanco) {

    /** 60 / 52,5 = 8 / 7: sete horas reais de madrugada valem oito. */
    public static final BigDecimal HORA_NOTURNA_CLT = BigDecimal.valueOf(8).divide(BigDecimal.valueOf(7), 12, RoundingMode.HALF_UP);

    public RegrasPontuacao {
        Objects.requireNonNull(jornada);
        Objects.requireNonNull(toleranciaDiaria);
        Objects.requireNonNull(pesoDomingo);
        Objects.requireNonNull(pesoFeriado);
        Objects.requireNonNull(fatorHoraNoturna);
        jornada = Map.copyOf(jornada);
    }

    /** Padrão combinado com a Infoway: 8h de segunda a sexta, 4h no sábado, pesos 1 e hora noturna da CLT. */
    public static RegrasPontuacao padrao() {
        Map<DayOfWeek, Duration> jornada = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek dia : DayOfWeek.values()) {
            jornada.put(dia, Duration.ofHours(8));
        }
        jornada.put(DayOfWeek.SATURDAY, Duration.ofHours(4));
        jornada.put(DayOfWeek.SUNDAY, Duration.ZERO);
        return new RegrasPontuacao(jornada, Duration.ofMinutes(10), BigDecimal.ONE, BigDecimal.ONE,
                HORA_NOTURNA_CLT, false);
    }

    public Duration jornadaDe(LocalDate data) {
        return jornada.getOrDefault(data.getDayOfWeek(), Duration.ZERO);
    }

    public RegrasPontuacao comPesos(BigDecimal domingo, BigDecimal feriado) {
        return new RegrasPontuacao(jornada, toleranciaDiaria, domingo, feriado, fatorHoraNoturna, compensarFaltaNoBanco);
    }

    public RegrasPontuacao comCompensacaoDeFalta(boolean compensar) {
        return new RegrasPontuacao(jornada, toleranciaDiaria, pesoDomingo, pesoFeriado, fatorHoraNoturna, compensar);
    }
}
