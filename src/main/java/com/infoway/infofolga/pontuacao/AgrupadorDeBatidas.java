package com.infoway.infofolga.pontuacao;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Separa as batidas de um colaborador por dia de trabalho. Em regra cada batida fica no dia do calendário,
 * mas quem entrou e ainda não saiu continua no mesmo turno: a saída às 02h de um plantão que começou às 22h
 * pertence ao dia em que o turno começou.
 */
public final class AgrupadorDeBatidas {

    /** Turno aberto há mais tempo que isso não recebe a próxima batida: é esquecimento, não plantão. */
    private static final Duration TURNO_MAXIMO = Duration.ofHours(14);

    private AgrupadorDeBatidas() {
    }

    public static Map<LocalDate, List<LocalDateTime>> porDia(List<LocalDateTime> batidas) {
        Map<LocalDate, List<LocalDateTime>> dias = new TreeMap<>();
        LocalDate diaAtual = null;
        LocalDateTime ultima = null;
        for (LocalDateTime batida : batidas.stream().sorted().toList()) {
            List<LocalDateTime> doDia = diaAtual == null ? null : dias.get(diaAtual);
            boolean turnoAberto = doDia != null && doDia.size() % 2 == 1;
            boolean continuaTurno = turnoAberto
                    && !batida.toLocalDate().equals(diaAtual)
                    && Duration.between(ultima, batida).compareTo(TURNO_MAXIMO) <= 0;
            if (!continuaTurno) {
                diaAtual = batida.toLocalDate();
            }
            dias.computeIfAbsent(diaAtual, d -> new ArrayList<>()).add(batida);
            ultima = batida;
        }
        return dias;
    }
}
