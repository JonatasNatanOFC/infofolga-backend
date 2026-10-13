package com.infoway.infofolga.ponto;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeitorAfdTest {

    // CPFs fictícios. Linhas no leiaute da Portaria 671: NSR(9) tipo(1) data-hora(24) CPF(12) e o resto.
    private static final String AFD = String.join("\n",
            "0000000001" + "1".repeat(290),                                 // cabeçalho (ignorado)
            "000000002" + "3" + "2026-10-13T08:02:00-0300" + "052998224725" + "A1B2",
            "000000003" + "3" + "2026-10-13T12:00:00-0300" + "52998224725 " + "C3D4",  // CPF com espaço no fim
            "000000004" + "7" + "2026-10-13T13:00:00-0300" + "071305777380"
                    + "2026-10-13T13:00:05-0300" + "01" + "0" + "f".repeat(64),         // REP-P
            "000000004" + "7" + "2026-10-13T13:00:00-0300" + "071305777380"
                    + "2026-10-13T13:00:05-0300" + "01" + "0" + "f".repeat(64),         // repetida
            "000000005" + "3" + "13102026" + "0800" + "012345678901",                  // Portaria 1510 (PIS)
            "000000006" + "3" + "data-quebrada-----------" + "052998224725" + "0000",
            "999999999" + "00000000000000000000000000000000000000000000000000000009");

    @Test
    void leMarcacoesDosTiposTresESeteEIgnoraORestante() throws IOException {
        LeitorAfd.Resultado resultado = LeitorAfd.ler(new StringReader(AFD));

        assertEquals(8, resultado.linhas());
        assertEquals(1, resultado.formatoAntigo());
        assertEquals(3, resultado.marcacoes().size());
        assertEquals(new Marcacao("52998224725", LocalDateTime.of(2026, 10, 13, 8, 2)), resultado.marcacoes().get(0));
        assertEquals("52998224725", resultado.marcacoes().get(1).cpf());
        assertEquals(new Marcacao("71305777380", LocalDateTime.of(2026, 10, 13, 13, 0)), resultado.marcacoes().get(2));
    }
}
