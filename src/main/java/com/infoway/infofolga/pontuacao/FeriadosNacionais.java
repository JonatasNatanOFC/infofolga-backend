package com.infoway.infofolga.pontuacao;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import java.util.TreeMap;

/**
 * Feriados nacionais (Lei 662/1949, Lei 6.802/1980 e Lei 14.759/2023). Carnaval e Corpus Christi são pontos
 * facultativos: a empresa cadastra no próprio calendário se costuma folgar.
 */
public final class FeriadosNacionais {

    private FeriadosNacionais() {
    }

    public static Map<LocalDate, String> doAno(int ano) {
        Map<LocalDate, String> feriados = new TreeMap<>();
        feriados.put(LocalDate.of(ano, Month.JANUARY, 1), "Confraternização Universal");
        feriados.put(pascoa(ano).minusDays(2), "Paixão de Cristo");
        feriados.put(LocalDate.of(ano, Month.APRIL, 21), "Tiradentes");
        feriados.put(LocalDate.of(ano, Month.MAY, 1), "Dia do Trabalho");
        feriados.put(LocalDate.of(ano, Month.SEPTEMBER, 7), "Independência do Brasil");
        feriados.put(LocalDate.of(ano, Month.OCTOBER, 12), "Nossa Senhora Aparecida");
        feriados.put(LocalDate.of(ano, Month.NOVEMBER, 2), "Finados");
        feriados.put(LocalDate.of(ano, Month.NOVEMBER, 15), "Proclamação da República");
        if (ano >= 2024) {
            feriados.put(LocalDate.of(ano, Month.NOVEMBER, 20), "Dia Nacional de Zumbi e da Consciência Negra");
        }
        feriados.put(LocalDate.of(ano, Month.DECEMBER, 25), "Natal");
        return feriados;
    }

    /** Domingo de Páscoa pelo algoritmo de Meeus/Jones/Butcher (calendário gregoriano). */
    static LocalDate pascoa(int ano) {
        int a = ano % 19;
        int b = ano / 100;
        int c = ano % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(ano, mes, dia);
    }
}
