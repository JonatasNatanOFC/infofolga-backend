package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.GerentePayload;
import com.infoway.infofolga.model.Gerente;
import com.infoway.infofolga.service.GerenciaAdministracaoService;
import com.infoway.infofolga.repository.GerenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gerencia")
public class GerenciaController {

    @Autowired
    private GerenciaAdministracaoService adminService;

    @Autowired
    private GerenteRepository gerenteRepository;

    @GetMapping("/gerentes")
    public ResponseEntity<List<Gerente>> listarGerentes() {
        return ResponseEntity.ok(gerenteRepository.findAll());
    }

    @PostMapping("/gerentes")
    public ResponseEntity<Void> criarGerente(@RequestBody GerentePayload payload) {
        adminService.criarGerente(payload);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/funcionarios/{id}/promover")
    public ResponseEntity<Void> promoverFuncionario(@PathVariable Long id) {
        adminService.promoverParaGerente(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/gerentes/{id}/rebaixar")
    public ResponseEntity<Void> rebaixarGerente(@PathVariable Long id) {
        adminService.rebaixarParaFuncionario(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/gerentes/{id}/inativar")
    public ResponseEntity<Void> inativarGerente(@PathVariable Long id) {
        adminService.inativarGerente(id);
        return ResponseEntity.ok().build();
    }
}