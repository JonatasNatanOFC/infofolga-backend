package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.CriarSolicitacaoDto;
import com.infoway.infofolga.dto.SolicitacaoDto;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.service.FuncionarioService;
import com.infoway.infofolga.service.FuncionarioSolicitacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/funcionarios/solicitacoes")
public class FuncionarioSolicitacaoController {

    private final FuncionarioSolicitacaoService funcionarioSolicitacaoService;
    private final FuncionarioService funcionarioService;

    public FuncionarioSolicitacaoController(
            FuncionarioSolicitacaoService funcionarioSolicitacaoService,
            FuncionarioService funcionarioService
    ) {
        this.funcionarioSolicitacaoService = funcionarioSolicitacaoService;
        this.funcionarioService = funcionarioService;
    }

    @PostMapping
    public ResponseEntity<SolicitacaoDto> criar(
            @RequestBody @Valid CriarSolicitacaoDto dto,
            Authentication authentication
    ) {
        Funcionario funcionario = funcionarioService.getFuncionarioAutenticado(authentication.getPrincipal());
        SolicitacaoDto criada = funcionarioSolicitacaoService.criarSolicitacao(dto, funcionario);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @GetMapping
    public ResponseEntity<List<SolicitacaoDto>> listarMinhas(Authentication authentication) {
        Funcionario funcionario = funcionarioService.getFuncionarioAutenticado(authentication.getPrincipal());
        return ResponseEntity.ok(
                funcionarioSolicitacaoService.listarMinhasSolicitacoes(funcionario.getId())
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id, Authentication authentication) {
        Funcionario funcionario = funcionarioService.getFuncionarioAutenticado(authentication.getPrincipal());
        funcionarioSolicitacaoService.cancelarSolicitacao(id, funcionario.getId());
        return ResponseEntity.noContent().build();
    }
}