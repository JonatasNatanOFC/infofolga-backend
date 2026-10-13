package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.ConfiguracaoPontuacaoDto;
import com.infoway.infofolga.dto.ExemploPontuacaoDto;
import com.infoway.infofolga.dto.FeriadoDto;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.service.ConfiguracaoPontuacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Configuração da pontuação: só o CEO e o RH alteram as regras e o calendário. */
@RestController
@RequestMapping("/api/pontuacao")
@PreAuthorize("hasAnyRole('CEO', 'RH')")
public class PontuacaoController {

    private final ConfiguracaoPontuacaoService service;

    public PontuacaoController(ConfiguracaoPontuacaoService service) {
        this.service = service;
    }

    @GetMapping("/configuracao")
    public ResponseEntity<List<ConfiguracaoPontuacaoDto>> historico() {
        return ResponseEntity.ok(service.historico());
    }

    @PostMapping("/configuracao")
    public ResponseEntity<ConfiguracaoPontuacaoDto> salvar(@RequestBody @Valid ConfiguracaoPontuacaoDto dto,
                                                           @AuthenticationPrincipal Colaborador logado) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(dto, logado));
    }

    /** Mostra o efeito das regras do formulário em dias de exemplo, antes de salvar. */
    @PostMapping("/simular")
    public ResponseEntity<List<ExemploPontuacaoDto>> simular(@RequestBody @Valid ConfiguracaoPontuacaoDto dto) {
        return ResponseEntity.ok(service.simular(dto));
    }

    @GetMapping("/feriados")
    public ResponseEntity<List<FeriadoDto>> feriados(@RequestParam int ano) {
        return ResponseEntity.ok(service.feriadosDoAno(ano));
    }

    @PostMapping("/feriados")
    public ResponseEntity<FeriadoDto> adicionarFeriado(@RequestBody @Valid FeriadoDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionarFeriado(dto));
    }

    @DeleteMapping("/feriados/{id}")
    public ResponseEntity<Void> removerFeriado(@PathVariable Long id) {
        service.removerFeriado(id);
        return ResponseEntity.noContent().build();
    }
}
