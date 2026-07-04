package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.UsuarioDto;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Gerente;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @GetMapping("/me")
    public ResponseEntity<?> getUsuarioLogado(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Funcionario funcionario) {
            return ResponseEntity.ok(new UsuarioDto(funcionario));
        }

        if (principal instanceof Gerente gerente) {
            return ResponseEntity.ok(gerente);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}