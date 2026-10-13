package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.AjusteSaldoDto;
import com.infoway.infofolga.dto.ExtratoDto;
import com.infoway.infofolga.dto.ResultadoImportacaoDto;
import com.infoway.infofolga.dto.SaldoColaboradorDto;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.service.BancoDeHorasService;
import com.infoway.infofolga.service.ExtratoService;
import com.infoway.infofolga.service.ImportacaoPontoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

/** Saldo e extrato de pontos: o funcionário vê o seu; CEO e RH veem os de todos os funcionários. */
@RestController
@RequestMapping("/api/pontos")
public class PontosController {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final ExtratoService service;
    private final ImportacaoPontoService importacao;
    private final BancoDeHorasService bancoDeHoras;

    public PontosController(ExtratoService service, ImportacaoPontoService importacao,
                            BancoDeHorasService bancoDeHoras) {
        this.service = service;
        this.importacao = importacao;
        this.bancoDeHoras = bancoDeHoras;
    }

    /**
     * Antes de pedir uma folga do banco: saldo disponível, quanto a folga usa e a regra da empresa.
     * 204 quando não se aplica (perfil sem banco de horas ou empresa sem regras).
     */
    @GetMapping("/previa-folga")
    public ResponseEntity<BancoDeHorasService.AvaliacaoFolga> previaFolga(
            @AuthenticationPrincipal Colaborador logado,
            @RequestParam java.time.LocalDate inicio, @RequestParam java.time.LocalDate fim) {
        if (fim.isBefore(inicio) || inicio.plusDays(366).isBefore(fim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período inválido.");
        }
        return bancoDeHoras.avaliarFolga(logado, inicio, fim)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
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

    @PostMapping("/{colaboradorId}/ajustes")
    @PreAuthorize("hasAnyRole('CEO', 'RH')")
    public ResponseEntity<AjusteSaldoDto> lancarAjuste(@PathVariable Long colaboradorId,
                                                       @RequestBody @Valid AjusteSaldoDto dto,
                                                       @AuthenticationPrincipal Colaborador logado) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.lancarAjuste(colaboradorId, dto, logado));
    }

    @DeleteMapping("/ajustes/{id}")
    @PreAuthorize("hasAnyRole('CEO', 'RH')")
    public ResponseEntity<Void> removerAjuste(@PathVariable Long id, @AuthenticationPrincipal Colaborador logado) {
        service.removerAjuste(id, logado);
        return ResponseEntity.noContent().build();
    }

    /** Arquivo AFD exportado do relógio de ponto (Portaria 671): grava as batidas e recalcula os dias. */
    @PostMapping("/importar-afd")
    @PreAuthorize("hasAnyRole('CEO', 'RH')")
    public ResponseEntity<ResultadoImportacaoDto> importarAfd(@RequestParam("arquivo") MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha o arquivo AFD.");
        }
        try {
            return ResponseEntity.ok(importacao.importarAfd(arquivo.getInputStream()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo.");
        }
    }

    private static YearMonth mesOuAtual(YearMonth mes) {
        return mes != null ? mes : YearMonth.now(FUSO);
    }
}
