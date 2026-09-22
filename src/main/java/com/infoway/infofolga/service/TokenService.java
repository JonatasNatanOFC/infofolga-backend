package com.infoway.infofolga.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.infoway.infofolga.model.Colaborador;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

    private static final String ISSUER = "API infofolga";
    private static final Duration VALIDADE = Duration.ofHours(2);
    private static final int TAMANHO_MINIMO_SECRET = 32;

    private final Algorithm algoritmo;
    private final JWTVerifier verifier;

    public TokenService(@Value("${api.security.token.secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SECRET) {
            throw new IllegalStateException(
                    "JWT_SECRET ausente ou fraco: use pelo menos " + TAMANHO_MINIMO_SECRET + " bytes aleatórios.");
        }
        this.algoritmo = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algoritmo)
                .withIssuer(ISSUER)
                .build();
    }

    public String gerarToken(Colaborador colaborador) {
        try {
            Instant agora = Instant.now();
            return JWT.create()
                    .withIssuer(ISSUER)
                    .withSubject(colaborador.getCpf())
                    .withIssuedAt(agora)
                    .withExpiresAt(agora.plus(VALIDADE))
                    .sign(algoritmo);
        } catch (JWTCreationException exception) {
            throw new IllegalStateException("Erro ao gerar token jwt", exception);
        }
    }

    public String validarToken(String tokenJWT) {
        try {
            return verifier.verify(tokenJWT).getSubject();
        } catch (JWTVerificationException exception) {
            return "";
        }
    }
}
