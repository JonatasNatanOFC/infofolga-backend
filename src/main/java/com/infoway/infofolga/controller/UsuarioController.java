package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.UsuarioDto;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.service.FuncionarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final FuncionarioService funcionarioService;

    public UsuarioController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioDto> getUsuarioLogado(Authentication authentication) {
        Funcionario funcionario = funcionarioService.getFuncionarioAutenticado(authentication.getPrincipal());
        return ResponseEntity.ok(new UsuarioDto(funcionario));
    }
}