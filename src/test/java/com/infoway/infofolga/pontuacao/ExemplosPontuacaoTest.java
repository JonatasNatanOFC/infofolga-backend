package com.infoway.infofolga.pontuacao;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExemplosPontuacaoTest {

    private static Map<String, BigDecimal> pontos(RegrasPontuacao regras) {
        List<ExemplosPontuacao.Exemplo> exemplos = ExemplosPontuacao.com(regras);
        return exemplos.stream().collect(Collectors.toMap(ExemplosPontuacao.Exemplo::titulo, e -> e.resultado().pontos()));
    }

    private static void assertPontos(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), () -> "esperado " + esperado + ", veio " + real);
    }

    @Test
    void regrasPadraoDaInfoway() {
        Map<String, BigDecimal> p = pontos(RegrasPontuacao.padrao());
        assertPontos("1", p.get("Segunda: saiu 1h depois"));
        assertPontos("0", p.get("Segunda: chegou 8 min atrasado"));
        assertPontos("-0.5", p.get("Segunda: chegou 30 min atrasado"));
        assertPontos("4", p.get("Domingo: 4h de plantão"));
        assertPontos("4", p.get("Feriado: 4h de plantão"));
        assertPontos("4.57", p.get("Domingo: chamado das 22h às 2h"));
        assertPontos("-8", p.get("Segunda: folga do banco de horas"));
        assertPontos("0", p.get("Segunda: falta sem justificativa"));
    }

    @Test
    void pesosECompensacaoMudamOsExemplos() {
        RegrasPontuacao regras = RegrasPontuacao.padrao()
                .comPesos(new BigDecimal("1.5"), new BigDecimal("2"))
                .comCompensacaoDeFalta(true);
        Map<String, BigDecimal> p = pontos(regras);
        assertPontos("6", p.get("Domingo: 4h de plantão"));
        assertPontos("8", p.get("Feriado: 4h de plantão"));
        assertPontos("-8", p.get("Segunda: falta sem justificativa"));
    }
}
