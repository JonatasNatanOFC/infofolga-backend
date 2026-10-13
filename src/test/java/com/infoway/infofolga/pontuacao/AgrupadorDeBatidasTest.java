package com.infoway.infofolga.pontuacao;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgrupadorDeBatidasTest {

    private static LocalDateTime t(String dataHora) {
        return LocalDateTime.parse(dataHora);
    }

    @Test
    void cadaBatidaFicaNoSeuDia() {
        Map<LocalDate, List<LocalDateTime>> dias = AgrupadorDeBatidas.porDia(List.of(
                t("2026-10-14T08:00"), t("2026-10-13T12:00"), t("2026-10-13T08:00"), t("2026-10-14T12:00")));
        assertEquals(2, dias.size());
        assertEquals(List.of(t("2026-10-13T08:00"), t("2026-10-13T12:00")), dias.get(LocalDate.of(2026, 10, 13)));
    }

    @Test
    void saidaDeMadrugadaFicaNoDiaEmQueOTurnoComecou() {
        Map<LocalDate, List<LocalDateTime>> dias = AgrupadorDeBatidas.porDia(List.of(
                t("2026-10-13T08:00"), t("2026-10-13T12:00"), t("2026-10-13T13:00"), t("2026-10-13T17:00"),
                t("2026-10-13T23:00"), t("2026-10-14T02:00"),
                t("2026-10-14T08:00"), t("2026-10-14T17:00")));
        assertEquals(6, dias.get(LocalDate.of(2026, 10, 13)).size());
        assertEquals(List.of(t("2026-10-14T08:00"), t("2026-10-14T17:00")), dias.get(LocalDate.of(2026, 10, 14)));
    }

    @Test
    void saidaEsquecidaNaoPuxaODiaSeguinte() {
        // Entrou às 08h e não bateu a saída: o dia seguinte começa normal e o dia 13 fica com batida faltando.
        Map<LocalDate, List<LocalDateTime>> dias = AgrupadorDeBatidas.porDia(List.of(
                t("2026-10-13T08:00"), t("2026-10-14T08:00"), t("2026-10-14T17:00")));
        assertEquals(1, dias.get(LocalDate.of(2026, 10, 13)).size());
        assertEquals(2, dias.get(LocalDate.of(2026, 10, 14)).size());
    }
}
