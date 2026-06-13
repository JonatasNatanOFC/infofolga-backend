package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.RejeitarSolicitacaoDto;
import com.infoway.infofolga.dto.SolicitacaoDto;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.service.GerenciaSolicitacaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gerencia")
public class GerenciaSolicitacaoController {

    private final GerenciaSolicitacaoService gerenciaSolicitacaoService;

    public GerenciaSolicitacaoController(GerenciaSolicitacaoService gerenciaSolicitacaoService) {
        this.gerenciaSolicitacaoService = gerenciaSolicitacaoService;
    }

    @GetMapping("/solicitacoes")
    public ResponseEntity<List<SolicitacaoDto>> getSolicitacoes() {
        return ResponseEntity.ok(gerenciaSolicitacaoService.listarSolicitacoes());
    }

    @GetMapping("/solicitacoes/status")
    public ResponseEntity<List<SolicitacaoDto>> getSolicitacoesPorStatus(
            @RequestParam StatusSolicitation status
    ) {
        return ResponseEntity.ok(gerenciaSolicitacaoService.listarPorStatus(status));
    }

    @GetMapping("/solicitacoes/funcionario/{funcionarioId}")
    public ResponseEntity<List<SolicitacaoDto>> getSolicitacoesByFuncionario(@PathVariable Long funcionarioId) {
        return ResponseEntity.ok(gerenciaSolicitacaoService.listarPorFuncionario(funcionarioId));
    }

    @PutMapping("/solicitacoes/{id}/aprovar")
    public ResponseEntity<SolicitacaoDto> aprovarSolicitacao(@PathVariable Long id) {
        return ResponseEntity.ok(gerenciaSolicitacaoService.aprovarSolicitacao(id));
    }

    @PutMapping("/solicitacoes/{id}/rejeitar")
    public ResponseEntity<SolicitacaoDto> rejeitarSolicitacao(
            @PathVariable Long id,
            @RequestBody @Valid RejeitarSolicitacaoDto dto
    ) {
        return ResponseEntity.ok(
                gerenciaSolicitacaoService.rejeitarSolicitacao(id, dto.motivo())
        );
    }

    @DeleteMapping("/solicitacoes/{id}")
    public ResponseEntity<Void> removerSolicitacao(@PathVariable Long id) {
        gerenciaSolicitacaoService.removerSolicitacao(id);
        return ResponseEntity.noContent().build();
    }
}