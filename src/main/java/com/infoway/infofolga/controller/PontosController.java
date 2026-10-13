package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.ExtratoDto;
import com.infoway.infofolga.dto.SaldoColaboradorDto;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.service.ExtratoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

/** Saldo e extrato de pontos: o funcionário vê o seu; CEO e RH veem os de todos os funcionários. */
@RestController
@RequestMapping("/api/pontos")
public class PontosController {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final ExtratoService service;

    public PontosController(ExtratoService service) {
        this.service = service;
    }

    /** mes no formato AAAA-MM; sem ele, o mês atual. */
    @GetMapping("/meus")
    public ResponseEntity<ExtratoDto> meus(@AuthenticationPrincipal Colaborador logado,
                                           @RequestParam(required = false) YearMonth mes) {
        return ResponseEntity.ok(service.meuExtrato(logado, mesOuAtual(mes)));
    }

    @GetMapping("/equipe")
    @PreAuthorize("hasAnyRole('CEO', 'RH')")
    public ResponseEntity<List<SaldoColaboradorDto>> equipe() {
        return ResponseEntity.ok(service.equipe());
    }

    @GetMapping("/{colaboradorId}")
    @PreAuthorize("hasAnyRole('CEO', 'RH')")
    public ResponseEntity<ExtratoDto> deColaborador(@PathVariable Long colaboradorId,
                                                    @RequestParam(required = false) YearMonth mes) {
        return ResponseEntity.ok(service.extratoDe(colaboradorId, mesOuAtual(mes)));
    }

    private static YearMonth mesOuAtual(YearMonth mes) {
        return mes != null ? mes : YearMonth.now(FUSO);
    }
}
