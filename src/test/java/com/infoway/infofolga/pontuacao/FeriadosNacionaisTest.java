package com.infoway.infofolga.pontuacao;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FeriadosNacionaisTest {

    @Test
    void sextaFeiraSantaAcompanhaAPascoa() {
        assertEquals("Paixão de Cristo", FeriadosNacionais.doAno(2026).get(LocalDate.of(2026, 4, 3)));
        assertEquals("Paixão de Cristo", FeriadosNacionais.doAno(2027).get(LocalDate.of(2027, 3, 26)));
        assertEquals("Paixão de Cristo", FeriadosNacionais.doAno(2019).get(LocalDate.of(2019, 4, 19)));
    }

    @Test
    void conscienciaNegraSoAPartirDe2024() {
        assertTrue(FeriadosNacionais.doAno(2026).containsKey(LocalDate.of(2026, 11, 20)));
        assertFalse(FeriadosNacionais.doAno(2023).containsKey(LocalDate.of(2023, 11, 20)));
    }

    @Test
    void carnavalNaoEFeriadoNacional() {
        Map<LocalDate, String> feriados2026 = FeriadosNacionais.doAno(2026);
        assertEquals(10, feriados2026.size());
        assertFalse(feriados2026.containsKey(LocalDate.of(2026, 2, 17)));
    }
}
