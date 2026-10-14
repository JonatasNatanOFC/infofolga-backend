package com.infoway.infofolga.service;

import com.auth0.jwt.JWT;
import com.infoway.infofolga.model.Colaborador;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private final TokenService tokens = new TokenService("segredo-de-teste-com-mais-de-32-bytes!!");

    @Test
    void tokenValeSeteDiasEIdentificaPeloCpf() {
        Colaborador c = new Colaborador();
        c.setCpf("52998224725");
        String token = tokens.gerarToken(c);

        assertEquals("52998224725", tokens.validarToken(token));
        Duration validade = Duration.between(Instant.now(), JWT.decode(token).getExpiresAtAsInstant());
        assertTrue(validade.compareTo(Duration.ofDays(6)) > 0 && validade.compareTo(Duration.ofDays(7)) <= 0);
    }

    @Test
    void tokenAlteradoNaoValida() {
        Colaborador c = new Colaborador();
        c.setCpf("52998224725");
        String token = tokens.gerarToken(c);
        assertEquals("", tokens.validarToken(token.substring(0, token.length() - 2) + "xx"));
    }
}
