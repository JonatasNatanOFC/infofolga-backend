package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.CadastroFuncionarioDto;
import com.infoway.infofolga.dto.UsuarioDto;
import com.infoway.infofolga.service.GerenciaFuncionarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gerencia")
public class GerenciaFuncionarioController {

    private final GerenciaFuncionarioService gerenciaFuncionarioService;

    public GerenciaFuncionarioController(GerenciaFuncionarioService gerenciaFuncionarioService) {
        this.gerenciaFuncionarioService = gerenciaFuncionarioService;
    }

    @GetMapping("/funcionarios")
    public ResponseEntity<List<UsuarioDto>> getFuncionarios() {
        return ResponseEntity.ok(gerenciaFuncionarioService.listarFuncionarios());
    }

    @GetMapping("/cpf-check")
    public ResponseEntity<Void> verificarCpf(@RequestParam String cpf) {
        return gerenciaFuncionarioService.existeCpf(cpf)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/cpf-buscar")
    public ResponseEntity<UsuarioDto> getFuncionarioByCpf(@RequestParam String cpf) {
        return ResponseEntity.ok(gerenciaFuncionarioService.buscarPorCpf(cpf));
    }

    @GetMapping("/funcionarios/{id}")
    public ResponseEntity<UsuarioDto> getFuncionario(@PathVariable Long id) {
        return ResponseEntity.ok(gerenciaFuncionarioService.buscarPorId(id));
    }

    @PostMapping("/funcionarios")
    public ResponseEntity<UsuarioDto> adicionarFuncionario(@RequestBody @Valid CadastroFuncionarioDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gerenciaFuncionarioService.adicionarFuncionario(dto));
    }

    @PutMapping("/funcionarios/{id}")
    public ResponseEntity<UsuarioDto> atualizarFuncionario(@PathVariable Long id,
                                                           @RequestBody @Valid CadastroFuncionarioDto dto) {
        return ResponseEntity.ok(gerenciaFuncionarioService.atualizarFuncionario(id, dto));
    }

    @DeleteMapping("/funcionarios/{id}")
    public ResponseEntity<Void> removerFuncionario(@PathVariable Long id) {
        gerenciaFuncionarioService.removerFuncionario(id);
        return ResponseEntity.noContent().build();
    }
}