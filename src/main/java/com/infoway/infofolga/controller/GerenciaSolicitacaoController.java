package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.SolicitacaoDto;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.service.GerenciaSolicitacaoService;
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

    @GetMapping("/solicitacoes/funcionario/{funcionarioId}")
    public ResponseEntity<List<SolicitacaoDto>> getSolicitacoesByFuncionario(@PathVariable Long funcionarioId) {
        return ResponseEntity.ok(gerenciaSolicitacaoService.listarPorFuncionario(funcionarioId));
    }

    @PatchMapping("/solicitacoes/{id}/status")
    public ResponseEntity<SolicitacaoDto> atualizarStatusSolicitacao(@PathVariable Long id,
                                                                     @RequestParam StatusSolicitation status) {
        return ResponseEntity.ok(gerenciaSolicitacaoService.atualizarStatus(id, status));
    }

    @DeleteMapping("/solicitacoes/{id}")
    public ResponseEntity<Void> removerSolicitacao(@PathVariable Long id) {
        gerenciaSolicitacaoService.removerSolicitacao(id);
        return ResponseEntity.noContent().build();
    }
}