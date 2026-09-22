package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.CriarSolicitacaoDto;
import com.infoway.infofolga.dto.PaginaDto;
import com.infoway.infofolga.dto.RejeitarSolicitacaoDto;
import com.infoway.infofolga.dto.SolicitacaoDto;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.model.TipoSolicitacao;
import com.infoway.infofolga.service.SolicitacaoService;
import com.infoway.infofolga.util.FotoUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitacoes")
public class SolicitacaoController {

    private final SolicitacaoService solicitacaoService;

    public SolicitacaoController(SolicitacaoService solicitacaoService) {
        this.solicitacaoService = solicitacaoService;
    }

    @PostMapping
    public ResponseEntity<SolicitacaoDto> criar(@RequestBody @Valid CriarSolicitacaoDto dto) {
        var solicitacao = solicitacaoService.criarSolicitacao(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SolicitacaoDto(solicitacao));
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<SolicitacaoDto>> listarMinhas() {
        List<Solicitacao> minhas = solicitacaoService.listarMinhasSolicitacoes();
        List<SolicitacaoDto> dtos = minhas.stream().map(SolicitacaoDto::new).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/todas")
    @PreAuthorize("hasRole('GERENTE') or hasRole('CEO')")
    public ResponseEntity<List<SolicitacaoDto>> listarTodas(
            @RequestParam(required = false) List<StatusSolicitation> status,
            @RequestParam(required = false) List<TipoSolicitacao> tipo) {
        List<Solicitacao> todas = solicitacaoService.listarTodasParaGerencia(status, tipo);
        List<SolicitacaoDto> dtos = todas.stream().map(SolicitacaoDto::new).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/gestao")
    @PreAuthorize("hasRole('GERENTE') or hasRole('CEO')")
    public ResponseEntity<PaginaDto<SolicitacaoDto>> listarPaginado(
            @RequestParam(required = false) List<StatusSolicitation> status,
            @RequestParam(required = false) List<TipoSolicitacao> tipo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        var page = solicitacaoService.paginarParaGerencia(status, tipo, pagina, tamanho);
        return ResponseEntity.ok(PaginaDto.de(page, SolicitacaoDto::new));
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<byte[]> foto(@PathVariable Long id, @AuthenticationPrincipal Colaborador logado) {
        return FotoUtils.resposta(solicitacaoService.buscarFotoHistorico(id, logado));
    }

    @PutMapping("/{id}/aprovar")
    @PreAuthorize("hasRole('GERENTE') or hasRole('CEO')")
    public ResponseEntity<SolicitacaoDto> aprovar(@PathVariable Long id, @AuthenticationPrincipal Colaborador avaliadorLogado) {
        var solicitacao = solicitacaoService.aprovarSolicitacao(id, avaliadorLogado.getId());
        return ResponseEntity.ok(new SolicitacaoDto(solicitacao));
    }

    @PutMapping("/{id}/rejeitar")
    @PreAuthorize("hasRole('GERENTE') or hasRole('CEO')")
    public ResponseEntity<SolicitacaoDto> rejeitar(
            @PathVariable Long id,
            @RequestBody @Valid RejeitarSolicitacaoDto dto,
            @AuthenticationPrincipal Colaborador avaliadorLogado) {
        var solicitacao = solicitacaoService.rejeitarSolicitacao(id, avaliadorLogado.getId(), dto.motivoResposta());
        return ResponseEntity.ok(new SolicitacaoDto(solicitacao));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        solicitacaoService.cancelarSolicitacao(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/invalidar")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    public ResponseEntity<SolicitacaoDto> invalidar(@PathVariable Long id) {
        var solicitacao = solicitacaoService.invalidarSolicitacao(id);
        return ResponseEntity.ok(new SolicitacaoDto(solicitacao));
    }

    @PutMapping("/{id}/usufruir")
    @PreAuthorize("hasAnyRole('FUNCIONARIO', 'GERENTE', 'CEO')")
    public ResponseEntity<SolicitacaoDto> usufruir(@PathVariable Long id) {
        var solicitacao = solicitacaoService.usufruirSolicitacao(id);
        return ResponseEntity.ok(new SolicitacaoDto(solicitacao));
    }

    @PutMapping("/{id}/aprovar-estorno")
    @PreAuthorize("hasRole('GERENTE') or hasRole('CEO')")
    public ResponseEntity<SolicitacaoDto> aprovarEstorno(@PathVariable Long id, @AuthenticationPrincipal Colaborador avaliadorLogado) {
        var solicitacao = solicitacaoService.aprovarEstorno(id, avaliadorLogado.getId());
        return ResponseEntity.ok(new SolicitacaoDto(solicitacao));
    }

    @PutMapping("/{id}/rejeitar-estorno")
    @PreAuthorize("hasRole('GERENTE') or hasRole('CEO')")
    public ResponseEntity<SolicitacaoDto> rejeitarEstorno(@PathVariable Long id, @AuthenticationPrincipal Colaborador avaliadorLogado) {
        var solicitacao = solicitacaoService.rejeitarEstorno(id, avaliadorLogado.getId());
        return ResponseEntity.ok(new SolicitacaoDto(solicitacao));
    }
}