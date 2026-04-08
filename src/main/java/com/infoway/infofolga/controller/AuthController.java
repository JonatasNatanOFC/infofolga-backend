package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.LoginRequestDto;
import com.infoway.infofolga.dto.LoginResponseDto;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
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
            var auth = this.authenticationManager.authenticate(usernamePassword);
            var funcionario = (Funcionario) auth.getPrincipal();
            var token = tokenService.gerarToken(funcionario);

            return ResponseEntity.ok(
                    new LoginResponseDto(
                            token,
                            funcionario.getNome(),
                            funcionario.getRole()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(403).body("Usuário ou senha inválidos.");
        } catch (Exception e) {
            System.err.println("==== ERRO REAL DE SERVIDOR ====");
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro interno: " + e.getMessage());
        }
    }
}