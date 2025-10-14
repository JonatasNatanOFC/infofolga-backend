package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.LoginRequestDto;
import com.infoway.infofolga.dto.LoginResponseDto;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody @Valid LoginRequestDto data) {
        System.out.println("!!!!!!!!! CHEGUEI NO AUTH CONTROLLER !!!!!!!!!");
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.matricula(), data.senha());
        try {
            var auth = this.authenticationManager.authenticate(usernamePassword);
            var funcionario = (Funcionario) auth.getPrincipal();
            var token = tokenService.gerarToken(funcionario);
            return ResponseEntity.ok(new LoginResponseDto(token, funcionario.getNome(), funcionario.getRole()));
        } catch (Exception e) {
            System.err.println("==== ERRO INESPERADO NO LOGIN ====");
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro interno no servidor: " + e.getMessage());
        }
    }
}