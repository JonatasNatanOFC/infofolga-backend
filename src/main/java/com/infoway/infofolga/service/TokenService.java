package com.infoway.infofolga.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.util.CpfUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {

    @Value("${JWT_SECRET}")
    private String secret;

    public String gerarToken(Funcionario funcionario) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            String cpfLimpo = CpfUtils.limpar(funcionario.getCpf());

            return JWT.create()
                    .withIssuer("infofolga-api")
                    .withSubject(cpfLimpo)
                    .withExpiresAt(Instant.now().plus(2, ChronoUnit.HOURS))
                    .sign(algorithm);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar token", e);
        }
    }

    public String validarToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            String subject = JWT.require(algorithm)
                    .withIssuer("infofolga-api")
                    .build()
                    .verify(token)
                    .getSubject();

            return CpfUtils.limpar(subject);
        } catch (JWTVerificationException e) {
            return null;
        }
    }
}