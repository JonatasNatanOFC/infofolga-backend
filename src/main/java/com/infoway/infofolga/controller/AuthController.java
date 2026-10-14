package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.LoginRequestDto;
import com.infoway.infofolga.dto.LoginResponseDto;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.service.AuthService;
import com.infoway.infofolga.service.TokenService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    /** Troca um token ainda válido por um novo, com mais 7 dias. Token vencido cai no 401 e pede login. */
    @PostMapping("/renovar")
    public ResponseEntity<LoginResponseDto> renovar(@AuthenticationPrincipal Colaborador logado) {
        return ResponseEntity.ok(new LoginResponseDto(tokenService.gerarToken(logado), logado.getNome(), logado.getRole()));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody @Valid LoginRequestDto data) {
        return ResponseEntity.ok(authService.login(data));
    }
}