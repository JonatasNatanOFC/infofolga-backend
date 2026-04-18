package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.FuncionarioStatsDto;
import com.infoway.infofolga.dto.UsuarioDto;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.service.FuncionarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/funcionarios")
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    public FuncionarioController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioDto> getMe(Authentication authentication) {
        System.out.println("\n=== CONTROLLER /api/funcionarios/me ===");
        System.out.println("AUTH: " + authentication);
        System.out.println("PRINCIPAL: " + (authentication != null ? authentication.getPrincipal() : null));
        System.out.println("=======================================\n");

        Funcionario funcionario = funcionarioService.getFuncionarioAutenticado(authentication.getPrincipal());
        return ResponseEntity.ok(new UsuarioDto(funcionario));
    }

    @GetMapping("/me/stats")
    public ResponseEntity<FuncionarioStatsDto> getMyStats(Authentication authentication) {
        System.out.println("\n=== CONTROLLER /api/funcionarios/me/stats ===");
        System.out.println("AUTH: " + authentication);
        System.out.println("PRINCIPAL: " + (authentication != null ? authentication.getPrincipal() : null));
        System.out.println("=============================================\n");

        Funcionario funcionario = funcionarioService.getFuncionarioAutenticado(authentication.getPrincipal());
        return ResponseEntity.ok(funcionarioService.getStats(funcionario.getId()));
    }
}