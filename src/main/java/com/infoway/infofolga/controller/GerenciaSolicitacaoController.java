package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.SolicitacaoDto;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.service.GerenciaSolicitacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gerencia/solicitacoes")
public class GerenciaSolicitacaoController {

    private final GerenciaSolicitacaoService gerenciaSolicitacaoService;

    public GerenciaSolicitacaoController(GerenciaSolicitacaoService gerenciaSolicitacaoService) {
        this.gerenciaSolicitacaoService = gerenciaSolicitacaoService;
    }

    @GetMapping
    public ResponseEntity<List<SolicitacaoDto>> listarTodas() {
        return ResponseEntity.ok(gerenciaSolicitacaoService.listarSolicitacoes());
    }

    @GetMapping("/status")
    public ResponseEntity<List<SolicitacaoDto>> listarPorStatus(@RequestParam StatusSolicitation status) {
        return ResponseEntity.ok(gerenciaSolicitacaoService.listarPorStatus(status));
    }

    @GetMapping("/funcionario/{funcionarioId}")
    public ResponseEntity<List<SolicitacaoDto>> listarPorFuncionario(@PathVariable Long funcionarioId) {
        return ResponseEntity.ok(gerenciaSolicitacaoService.listarPorFuncionario(funcionarioId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removerSolicitacao(@PathVariable Long id) {
        gerenciaSolicitacaoService.removerSolicitacao(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/aprovar")
    public ResponseEntity<SolicitacaoDto> aprovar(@PathVariable Long id) {
        return ResponseEntity.ok(gerenciaSolicitacaoService.aprovarSolicitacao(id));
    }

    @PutMapping("/{id}/rejeitar")
    public ResponseEntity<SolicitacaoDto> rejeitar(@PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String motivo = (body != null && body.containsKey("motivo")) ? body.get("motivo") : "Sem motivo informado";
        return ResponseEntity.ok(gerenciaSolicitacaoService.rejeitarSolicitacao(id, motivo));
    }

    @PutMapping("/{id}/invalidar")
    public ResponseEntity<SolicitacaoDto> invalidar(@PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String motivo = (body != null && body.containsKey("motivo") && !body.get("motivo").trim().isEmpty())
                ? body.get("motivo")
                : "Aprovado pelo gerente sem comentários adicionais.";

        return ResponseEntity.ok(gerenciaSolicitacaoService.invalidarSolicitacao(id, motivo));
    }

    @PutMapping("/{id}/usufruir")
    public ResponseEntity<SolicitacaoDto> usufruir(@PathVariable Long id) {
        return ResponseEntity.ok(gerenciaSolicitacaoService.usufruirSolicitacao(id));
    }
}