package com.infoway.infofolga.pontuacao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** Casos combinados com o RH: jornada 8h (seg–sex) e 4h (sáb), 1 ponto por hora, tolerância de 10 min. */
class ApuracaoDiariaTest {

    private static final LocalDate TERCA = LocalDate.of(2026, 10, 13);
    private static final LocalDate SABADO = LocalDate.of(2026, 10, 17);
    private static final LocalDate DOMINGO = LocalDate.of(2026, 10, 18);
    private static final RegrasPontuacao PADRAO = RegrasPontuacao.padrao();

    static Stream<Arguments> diasDeTrabalho() {
        return Stream.of(
                arguments("jornada exata", dia(TERCA, "08:00", "12:00", "13:00", "17:00"), "0"),
                arguments("6 min a mais: dentro da tolerância", dia(TERCA, "08:00", "12:00", "13:00", "17:06"), "0"),
                arguments("10 min a mais: ainda na tolerância", dia(TERCA, "08:00", "12:00", "13:00", "17:10"), "0"),
                arguments("17 min a mais: conta tudo (Súmula 366)", dia(TERCA, "07:58", "12:00", "12:50", "17:05"), "0.28"),
                arguments("atraso de 20 min desconta", dia(TERCA, "08:20", "12:00", "13:00", "17:00"), "-0.33"),
                arguments("voltar 15 min antes do almoço é hora extra", dia(TERCA, "08:00", "12:00", "12:45", "17:00"), "0.25"),
                arguments("sábado de 4h", dia(SABADO, "08:00", "12:00"), "0"),
                arguments("sábado de 6h", dia(SABADO, "08:00", "14:00"), "2.00"),
                arguments("domingo: toda hora é extra", dia(DOMINGO, "09:00", "12:00"), "3.00"),
                arguments("chamado de madrugada (23h–02h) vale hora noturna",
                        dia(TERCA, "08:00", "12:00", "13:00", "17:00", "23:00", "02:00"), "3.43"),
                arguments("noite inteira prorrogada até 6h (Súmula 60)", dia(TERCA, "22:00", "06:00"), "1.14"),
                arguments("batidas fora de ordem são ordenadas",
                        new DiaTrabalho(TERCA, List.of(TERCA.atTime(13, 0), TERCA.atTime(8, 0),
                                TERCA.atTime(17, 30), TERCA.atTime(12, 0)), SituacaoDia.NORMAL), "0.50"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("diasDeTrabalho")
    void calculaOSaldoDoDia(String caso, DiaTrabalho dia, String pontosEsperados) {
        assertPontos(pontosEsperados, new ApuracaoDiaria(PADRAO).apurar(dia));
    }

    static Stream<Arguments> diasSemTrabalho() {
        return Stream.of(
                arguments(SituacaoDia.FERIADO, TERCA, "0"),
                arguments(SituacaoDia.FOLGA_COM_DESCONTO, TERCA, "-8.00"),
                arguments(SituacaoDia.FOLGA_COM_DESCONTO, SABADO, "-4.00"),
                arguments(SituacaoDia.FOLGA_SEM_DESCONTO, TERCA, "0"),
                arguments(SituacaoDia.FERIAS, TERCA, "0"),
                arguments(SituacaoDia.FALTA_JUSTIFICADA, TERCA, "0"),
                arguments(SituacaoDia.FALTA_INJUSTIFICADA, TERCA, "0"));
    }

    @ParameterizedTest(name = "{0} em {1}")
    @MethodSource("diasSemTrabalho")
    void soAFolgaDeBancoDeHorasDesconta(SituacaoDia situacao, LocalDate data, String pontosEsperados) {
        assertPontos(pontosEsperados, new ApuracaoDiaria(PADRAO).apurar(new DiaTrabalho(data, List.of(), situacao)));
    }

    @Test
    void faltaSemJustificativaSoDescontaSeAEmpresaCompensarNoBanco() {
        var falta = new DiaTrabalho(TERCA, List.of(), SituacaoDia.FALTA_INJUSTIFICADA);
        assertPontos("-8.00", new ApuracaoDiaria(PADRAO.comCompensacaoDeFalta(true)).apurar(falta));
    }

    @Test
    void pesoDeDomingoEFeriadoMultiplicaSoAsHorasAMais() {
        var regras = PADRAO.comPesos(new BigDecimal("1.5"), new BigDecimal("2"));
        var apuracao = new ApuracaoDiaria(regras);
        assertPontos("4.50", apuracao.apurar(dia(DOMINGO, "09:00", "12:00")));
        assertPontos("8.00", apuracao.apurar(new DiaTrabalho(TERCA,
                List.of(TERCA.atTime(8, 0), TERCA.atTime(12, 0)), SituacaoDia.FERIADO)));
        // Atraso em dia comum continua 1 para 1.
        assertPontos("-0.33", apuracao.apurar(dia(TERCA, "08:20", "12:00", "13:00", "17:00")));
    }

    @Test
    void batidaFaltandoDeixaODiaSemPontosAteACorrecao() {
        ResultadoDia resultado = new ApuracaoDiaria(PADRAO).apurar(dia(TERCA, "08:00", "12:00", "13:00"));
        assertTrue(resultado.inconsistente());
        assertPontos("0", resultado);
    }

    @Test
    void extratoExplicaODia() {
        var apuracao = new ApuracaoDiaria(PADRAO);
        assertEquals("Dia normal: trabalhou 8h17 de 8h00 previstas",
                apuracao.apurar(dia(TERCA, "07:58", "12:00", "12:50", "17:05")).descricao());
        assertEquals("Folga (banco de horas): 8h00 descontadas",
                apuracao.apurar(new DiaTrabalho(TERCA, List.of(), SituacaoDia.FOLGA_COM_DESCONTO)).descricao());
    }

    /** Monta as batidas de um turno; um horário menor que o anterior passa para o dia seguinte (madrugada). */
    private static DiaTrabalho dia(LocalDate data, String... horarios) {
        List<LocalDateTime> batidas = new ArrayList<>();
        LocalDate diaDaBatida = data;
        LocalTime anterior = null;
        for (String horario : horarios) {
            LocalTime hora = LocalTime.parse(horario);
            if (anterior != null && hora.isBefore(anterior)) {
                diaDaBatida = diaDaBatida.plusDays(1);
            }
            batidas.add(diaDaBatida.atTime(hora));
            anterior = hora;
        }
        return new DiaTrabalho(data, batidas, SituacaoDia.NORMAL);
    }

    private static void assertPontos(String esperado, ResultadoDia resultado) {
        assertEquals(0, new BigDecimal(esperado).compareTo(resultado.pontos()),
                () -> "esperado " + esperado + ", veio " + resultado.pontos() + " (" + resultado.descricao() + ")");
    }
}
