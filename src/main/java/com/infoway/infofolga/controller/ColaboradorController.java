package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.AtualizarPerfilDto;
import com.infoway.infofolga.dto.CadastroColaboradorDto;
import com.infoway.infofolga.dto.UsuarioDto;
import com.infoway.infofolga.dto.UsuarioResumoDto;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.model.Role;
import com.infoway.infofolga.service.ColaboradorService;
import com.infoway.infofolga.util.FotoUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<UsuarioDto> getMe(@AuthenticationPrincipal Colaborador colaborador) {
        return ResponseEntity.ok(new UsuarioDto(colaborador));
    }

    @PutMapping("/me")
    public ResponseEntity<UsuarioDto> atualizarMeuPerfil(@AuthenticationPrincipal Colaborador colaborador,
                                                         @RequestBody @Valid AtualizarPerfilDto dto) {
        return ResponseEntity.ok(colaboradorService.atualizarPerfil(colaborador.getId(), dto));
    }

    @GetMapping
    @PreAuthorize("hasRole('GERENTE') or hasRole('CEO')")
    public ResponseEntity<List<UsuarioResumoDto>> listarTodos() {
        List<UsuarioResumoDto> colaboradores = colaboradorService.listarTodos();
        return ResponseEntity.ok(colaboradores);
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<byte[]> foto(@PathVariable Long id, @AuthenticationPrincipal Colaborador logado) {
        boolean gestor = logado.getRole() == Role.CEO || logado.getRole() == Role.GERENTE;
        if (!gestor && !logado.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }
        return FotoUtils.resposta(colaboradorService.buscarFoto(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE') or hasRole('CEO')")
    public ResponseEntity<UsuarioDto> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(colaboradorService.buscarPorId(id));
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
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<Void> promover(@PathVariable Long id) {
        colaboradorService.promoverParaGerente(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/rebaixar")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<Void> rebaixar(@PathVariable Long id) {
        colaboradorService.rebaixarParaFuncionario(id);
        return ResponseEntity.ok().build();
    }
}