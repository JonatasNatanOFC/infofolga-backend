package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.UsuarioDto;
import com.infoway.infofolga.model.Funcionario;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    @GetMapping("/me")
    public ResponseEntity<UsuarioDto> getUsuarioLogado(Authentication authentication) {
        Funcionario funcionarioLogado = (Funcionario) authentication.getPrincipal();
        return ResponseEntity.ok(new UsuarioDto(funcionarioLogado));
    }
}