package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.GerentePayload;
import com.infoway.infofolga.service.GerenciaAdministracaoService;
import com.infoway.infofolga.repository.GerenteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gerencia")
public class GerenciaController {

    private final GerenciaAdministracaoService adminService;
    private final GerenteRepository gerenteRepository;

    public GerenciaController(GerenciaAdministracaoService adminService, GerenteRepository gerenteRepository) {
        this.adminService = adminService;
        this.gerenteRepository = gerenteRepository;
    }

    @GetMapping("/gerentes")
    public ResponseEntity<List<Map<String, Object>>> listarGerentes() {
        List<Map<String, Object>> gerentesSeguros = gerenteRepository.findAll().stream()
                .filter(g -> !g.getCpf().startsWith("REBAIXADO_"))
                .map(g -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", g.getId());
                    dto.put("nome", g.getNome());
                    dto.put("cpf", g.getCpf());
                    dto.put("status", g.getStatus());
                    dto.put("isCeo", g.isCeo());
                    dto.put("matricula", g.getMatricula());
                    dto.put("cargo", g.getCargo());
                    dto.put("setor", g.getSetor());
                    dto.put("foto", g.getFoto());

                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(gerentesSeguros);
    }

    @PostMapping("/gerentes")
    public ResponseEntity<Void> criarGerente(@RequestBody GerentePayload payload) {
        adminService.criarGerente(payload);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/funcionarios/{id}/promover")
    public ResponseEntity<Void> promoverFuncionario(@PathVariable Long id) {
        adminService.promoverParaGerente(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/gerentes/{id}/rebaixar")
    public ResponseEntity<Void> rebaixarGerente(@PathVariable Long id) {
        adminService.rebaixarParaFuncionario(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/gerentes/{id}/inativar")
    public ResponseEntity<Void> inativarGerente(@PathVariable Long id) {
        adminService.inativarGerente(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/gerentes/{id}")
    public ResponseEntity<Void> atualizarGerente(@PathVariable Long id, @RequestBody GerentePayload payload) {
        adminService.atualizarGerente(id, payload);
        return ResponseEntity.ok().build();
    }
}