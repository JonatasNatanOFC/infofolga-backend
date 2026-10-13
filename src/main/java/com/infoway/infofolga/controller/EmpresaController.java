package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.EmpresaDto;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.repository.EmpresaRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/empresa")
public class EmpresaController {

    private final EmpresaRepository empresaRepository;

    public EmpresaController(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    /** Nome e logos da empresa do usuário logado, para o app exibir a marca de cada cliente. */
    @GetMapping
    public ResponseEntity<EmpresaDto> minhaEmpresa(@AuthenticationPrincipal Colaborador logado) {
        return empresaRepository.findById(logado.getEmpresaId())
                .map(empresa -> ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                        .body(new EmpresaDto(empresa)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
