package com.infoway.infofolga.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.infoway.infofolga.model.Funcionario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String gerarToken(Funcionario usuario) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.create()
                    .withIssuer("infofolga-api")
                    .withSubject(usuario.getCpf())
                    .withClaim("nome", usuario.getNome())
                    .withClaim("role", usuario.getRole().name())
                    .withExpiresAt(getExpirationDate())
                    .sign(algorithm);

        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    public String validarToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.require(algorithm)
                    .withIssuer("infofolga-api")
                    .build()
                    .verify(token)
                    .getSubject();

        } catch (JWTVerificationException exception) {
            System.err.println("Token inválido: " + exception.getMessage());
            return null;
        }
    }

    private Instant getExpirationDate() {
        // Usa ZoneId dinâmico para respeitar horário de verão
        return LocalDateTime.now()
                .plusHours(8)
                .atZone(ZoneId.of("America/Sao_Paulo"))
                .toInstant();
    }
}