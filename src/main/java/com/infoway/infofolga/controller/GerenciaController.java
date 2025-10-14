package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.DashboardStatsDto;
import com.infoway.infofolga.repository.FuncionarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gerencia")
public class GerenciaController {
    @Autowired
    private FuncionarioRepository funcionarioRepository;
    // Futuramente, injete aqui o repositório de solicitações

    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasRole('ROLE_GERENTE')")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        // Lógica de exemplo:
        long totalFuncionarios = funcionarioRepository.count();
        long solicitacoesPendentes = 0; // Exemplo, viria do SolicitacaoRepository
        long totalSolicitacoes = 0;     // Exemplo, viria do SolicitacaoRepository

        var stats = new DashboardStatsDto(solicitacoesPendentes, totalFuncionarios, totalSolicitacoes);
        return ResponseEntity.ok(stats);
    }
}