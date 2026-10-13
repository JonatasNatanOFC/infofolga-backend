package com.infoway.infofolga.pontuacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Calcula o saldo de horas (pontos) de um colaborador em um dia.
 * Classe sem estado e sem acesso a banco: recebe as batidas e as regras, devolve o resultado.
 */
public final class ApuracaoDiaria {

    private static final LocalTime INICIO_NOTURNO = LocalTime.of(22, 0);
    private static final LocalTime FIM_NOTURNO = LocalTime.of(5, 0);
    private static final BigDecimal SEGUNDOS_POR_HORA = BigDecimal.valueOf(3600);

    private final RegrasPontuacao regras;

    public ApuracaoDiaria(RegrasPontuacao regras) {
        this.regras = Objects.requireNonNull(regras);
    }

    public ResultadoDia apurar(DiaTrabalho dia) {
        if (dia.situacao() == SituacaoDia.FALTA_INJUSTIFICADA && !regras.compensarFaltaNoBanco()) {
            return semPontos(dia, "Falta sem justificativa (descontada em folha)");
        }

        List<LocalDateTime> batidas = dia.batidas().stream().sorted().toList();
        if (batidas.size() % 2 != 0) {
            // Até o RH corrigir no ponto: o dia é recalculado sozinho nas próximas importações.
            return new ResultadoDia(dia.data(), dia.situacao(), 0, segundosPrevistos(dia), BigDecimal.ZERO, true,
                    "Batida faltando: aguardando correção do ponto");
        }

        long previsto = segundosPrevistos(dia);
        BigDecimal trabalhado = segundosTrabalhados(batidas);
        BigDecimal diferenca = trabalhado.subtract(BigDecimal.valueOf(previsto));

        // shortcut: aplica só o limite diário de 10 min; a CLT também limita 5 min por marcação, o que exige
        // o horário de cada batida na escala. Ajustar quando a escala vier da API do Faceponto.
        boolean dentroDaTolerancia = previsto > 0 && diferenca.signum() != 0
                && diferenca.abs().compareTo(BigDecimal.valueOf(regras.toleranciaDiaria().toSeconds())) <= 0;
        if (dentroDaTolerancia) {
            // Súmula 366 do TST: passando do limite, conta a diferença inteira, não só o excesso.
            diferenca = BigDecimal.ZERO;
        }

        // O peso de domingo/feriado só multiplica hora a mais; desconto vale sempre 1 para 1.
        BigDecimal peso = diferenca.signum() > 0 ? pesoDoDia(dia) : BigDecimal.ONE;
        BigDecimal pontos = diferenca.multiply(peso).divide(SEGUNDOS_POR_HORA, 2, RoundingMode.HALF_UP);

        long segundosTrabalhados = trabalhado.setScale(0, RoundingMode.HALF_UP).longValueExact();
        return new ResultadoDia(dia.data(), dia.situacao(), segundosTrabalhados, previsto, pontos, false,
                descrever(dia, segundosTrabalhados, previsto, peso, dentroDaTolerancia));
    }

    private long segundosPrevistos(DiaTrabalho dia) {
        return switch (dia.situacao()) {
            case NORMAL, FOLGA_COM_DESCONTO, FALTA_INJUSTIFICADA -> regras.jornadaDe(dia.data()).toSeconds();
            case FERIADO, FOLGA_SEM_DESCONTO, FERIAS, FALTA_JUSTIFICADA -> 0;
        };
    }

    /** Soma dos períodos entre batidas (entrada, saída, entrada, saída...), com a hora noturna convertida. */
    private BigDecimal segundosTrabalhados(List<LocalDateTime> batidas) {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < batidas.size(); i += 2) {
            LocalDateTime entrada = batidas.get(i);
            LocalDateTime saida = batidas.get(i + 1);
            long periodo = Duration.between(entrada, saida).toSeconds();
            long noturno = segundosNoturnos(entrada, saida);
            total = total
                    .add(BigDecimal.valueOf(periodo - noturno))
                    .add(BigDecimal.valueOf(noturno).multiply(regras.fatorHoraNoturna()));
        }
        return total;
    }

    /**
     * Segundos do período entre 22h e 5h. Se o período cobre a noite inteira e continua depois das 5h,
     * as horas seguintes também são noturnas (Súmula 60 do TST, prorrogação da jornada noturna).
     */
    private static long segundosNoturnos(LocalDateTime entrada, LocalDateTime saida) {
        long noturno = 0;
        for (LocalDate dia = entrada.toLocalDate().minusDays(1); !dia.isAfter(saida.toLocalDate()); dia = dia.plusDays(1)) {
            LocalDateTime inicioNoite = dia.atTime(INICIO_NOTURNO);
            LocalDateTime fimNoite = dia.plusDays(1).atTime(FIM_NOTURNO);
            LocalDateTime inicio = entrada.isAfter(inicioNoite) ? entrada : inicioNoite;
            LocalDateTime fim = saida.isBefore(fimNoite) ? saida : fimNoite;
            if (inicio.isBefore(fim)) {
                noturno += Duration.between(inicio, fim).toSeconds();
            }
            boolean cobriuANoiteInteira = !entrada.isAfter(inicioNoite) && saida.isAfter(fimNoite);
            if (cobriuANoiteInteira) {
                LocalDateTime proximaNoite = dia.plusDays(1).atTime(INICIO_NOTURNO);
                LocalDateTime fimProrrogacao = saida.isBefore(proximaNoite) ? saida : proximaNoite;
                noturno += Duration.between(fimNoite, fimProrrogacao).toSeconds();
            }
        }
        return noturno;
    }

    private BigDecimal pesoDoDia(DiaTrabalho dia) {
        BigDecimal peso = BigDecimal.ONE;
        if (dia.situacao() == SituacaoDia.FERIADO) {
            peso = peso.max(regras.pesoFeriado());
        }
        if (dia.data().getDayOfWeek() == DayOfWeek.SUNDAY) {
            peso = peso.max(regras.pesoDomingo());
        }
        return peso;
    }

    private static ResultadoDia semPontos(DiaTrabalho dia, String descricao) {
        return new ResultadoDia(dia.data(), dia.situacao(), 0, 0, BigDecimal.ZERO, false, descricao);
    }

    private static String descrever(DiaTrabalho dia, long trabalhado, long previsto, BigDecimal peso,
                                    boolean dentroDaTolerancia) {
        StringBuilder texto = new StringBuilder(rotulo(dia, previsto));
        if (trabalhado == 0 && previsto > 0) {
            texto.append(": ").append(horas(previsto)).append(" descontadas");
        } else if (trabalhado > 0) {
            texto.append(": trabalhou ").append(horas(trabalhado));
            if (previsto > 0) {
                texto.append(" de ").append(horas(previsto)).append(" previstas");
            }
        }
        if (dentroDaTolerancia) {
            texto.append(" (dentro da tolerância)");
        }
        if (peso.compareTo(BigDecimal.ONE) > 0) {
            texto.append(" · peso ").append(peso.stripTrailingZeros().toPlainString().replace('.', ','));
        }
        return texto.toString();
    }

    private static String rotulo(DiaTrabalho dia, long previsto) {
        if (dia.situacao() == SituacaoDia.NORMAL && previsto == 0) {
            return dia.data().getDayOfWeek() == DayOfWeek.SUNDAY ? "Domingo" : "Dia sem jornada";
        }
        return dia.situacao().rotulo();
    }

    private static String horas(long segundos) {
        long minutos = Math.round(segundos / 60.0);
        return String.format("%dh%02d", minutos / 60, minutos % 60);
    }
}
