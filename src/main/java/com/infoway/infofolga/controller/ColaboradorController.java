package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.CadastroColaboradorDto;
import com.infoway.infofolga.dto.ColaboradorStatsDto;
import com.infoway.infofolga.dto.UsuarioDto;
import com.infoway.infofolga.dto.UsuarioResumoDto;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.service.ColaboradorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/colaboradores")
public class ColaboradorController {

    private final ColaboradorService colaboradorService;

    ColaboradorController(ColaboradorService colaboradorService) {
        this.colaboradorService = colaboradorService;
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioDto> getMe(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        Colaborador colaborador = (Colaborador) authentication.getPrincipal();
        return ResponseEntity.ok(new UsuarioDto(colaborador));
    }

    @GetMapping("/me/stats")
    public ResponseEntity<ColaboradorStatsDto> getMyStats(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        Colaborador colaborador = (Colaborador) authentication.getPrincipal();
        return ResponseEntity.ok(colaboradorService.getStats(colaborador.getId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('GERENTE') or hasRole('CEO')")
    public ResponseEntity<List<UsuarioResumoDto>> listarTodos() {
        List<UsuarioResumoDto> colaboradores = colaboradorService.listarTodos();
        return ResponseEntity.ok(colaboradores);
    }

    @PostMapping
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<UsuarioDto> cadastrar(@RequestBody @Valid CadastroColaboradorDto dto) {
        UsuarioDto novoColaborador = colaboradorService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoColaborador);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<UsuarioDto> atualizar(@PathVariable Long id, @RequestBody CadastroColaboradorDto dto) {
        UsuarioDto atualizado = colaboradorService.atualizar(id, dto);
        return ResponseEntity.ok(atualizado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        colaboradorService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/inativar")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        colaboradorService.inativar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reativar")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<Void> reativar(@PathVariable Long id) {
        colaboradorService.reativar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/promover")
    public ResponseEntity<Void> promover(@PathVariable Long id) {
        colaboradorService.promoverParaGerente(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/rebaixar")
    public ResponseEntity<Void> rebaixar(@PathVariable Long id) {
        colaboradorService.rebaixarParaFuncionario(id);
        return ResponseEntity.ok().build();
    }
}