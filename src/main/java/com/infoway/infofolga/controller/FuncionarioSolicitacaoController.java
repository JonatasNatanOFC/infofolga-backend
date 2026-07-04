package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.CriarSolicitacaoDto;
import com.infoway.infofolga.dto.SolicitacaoDto;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Gerente;
import com.infoway.infofolga.repository.GerenteRepository;
import com.infoway.infofolga.service.FuncionarioService;
import com.infoway.infofolga.service.FuncionarioSolicitacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/funcionarios/solicitacoes")
public class FuncionarioSolicitacaoController {

    private final FuncionarioSolicitacaoService funcionarioSolicitacaoService;
    private final FuncionarioService funcionarioService;
    private final GerenteRepository gerenteRepository;

    public FuncionarioSolicitacaoController(
            FuncionarioSolicitacaoService funcionarioSolicitacaoService,
            FuncionarioService funcionarioService,
            GerenteRepository gerenteRepository) {
        this.funcionarioSolicitacaoService = funcionarioSolicitacaoService;
        this.funcionarioService = funcionarioService;
        this.gerenteRepository = gerenteRepository;
    }

    @PostMapping
    public ResponseEntity<SolicitacaoDto> criar(
            @RequestBody @Valid CriarSolicitacaoDto dto,
            Authentication authentication) {
        Optional<Gerente> optGerente = gerenteRepository.findByCpf(authentication.getName());
        if (optGerente.isPresent()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Acesso Negado. Apenas funcionários podem solicitar folgas.");
        }

        Funcionario funcionario = funcionarioService.getFuncionarioAutenticado(authentication.getPrincipal());
        SolicitacaoDto criada = funcionarioSolicitacaoService.criarSolicitacao(dto, funcionario);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @GetMapping
    public ResponseEntity<List<SolicitacaoDto>> listarMinhas(Authentication authentication) {
        Optional<Gerente> optGerente = gerenteRepository.findByCpf(authentication.getName());
        if (optGerente.isPresent()) {
            return ResponseEntity.ok(List.of());
        }

        Funcionario funcionario = funcionarioService.getFuncionarioAutenticado(authentication.getPrincipal());
        return ResponseEntity.ok(funcionarioSolicitacaoService.listarMinhasSolicitacoes(funcionario.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id, Authentication authentication) {
        Optional<Gerente> optGerente = gerenteRepository.findByCpf(authentication.getName());
        if (optGerente.isPresent()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso Negado.");
        }

        Funcionario funcionario = funcionarioService.getFuncionarioAutenticado(authentication.getPrincipal());
        funcionarioSolicitacaoService.cancelarSolicitacao(id, funcionario.getId());
        return ResponseEntity.noContent().build();
    }
}