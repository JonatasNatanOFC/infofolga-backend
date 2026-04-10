package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.LoginRequestDto;
import com.infoway.infofolga.dto.LoginResponseDto;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.service.TokenService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthController(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDto data) {

        var usernamePassword = new UsernamePasswordAuthenticationToken(
                data.cpf(),
                data.senha());

        try {
            log.info("[AuthController] Tentativa de login para CPF: {}", data.cpf());
            var auth = this.authenticationManager.authenticate(usernamePassword);
            var funcionario = (Funcionario) auth.getPrincipal();
            var token = tokenService.gerarToken(funcionario);

            log.info("[AuthController] Login OK para CPF: {} | ROLE: {}", data.cpf(), funcionario.getRole());
            return ResponseEntity.ok(
                    new LoginResponseDto(
                            token,
                            funcionario.getNome(),
                            funcionario.getRole()));
        } catch (AuthenticationException e) {
            log.warn("[AuthController] Auth falhou ({}) para CPF: {}", e.getClass().getSimpleName(), data.cpf());
            return ResponseEntity.status(403).body("Usuário ou senha inválidos.");
        } catch (Exception e) {
            log.error("[AuthController] Erro inesperado no login", e);
            return ResponseEntity.status(500).body("Erro interno: " + e.getMessage());
        }
    }
}