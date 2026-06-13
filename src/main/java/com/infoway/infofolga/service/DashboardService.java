package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.DashboardStatsDto;
import com.infoway.infofolga.model.Role;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.repository.FuncionarioRepository;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DashboardService {

    private final FuncionarioRepository funcionarioRepository;
    private final SolicitacaoRepository solicitacaoRepository;

    public DashboardService(FuncionarioRepository funcionarioRepository,
                            SolicitacaoRepository solicitacaoRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.solicitacaoRepository = solicitacaoRepository;
    }

    public DashboardStatsDto getDashboardStats() {
        long totalFuncionarios = funcionarioRepository.count();
        long solicitacoesPendentes = solicitacaoRepository.countByStatus(StatusSolicitation.PENDENTE);
        LocalDateTime trintaDiasAtras = LocalDateTime.now().minusDays(30);
        long aprovadas30Dias = solicitacaoRepository.countByStatusAndAtualizadoEmAfter(
                StatusSolicitation.APROVADA, trintaDiasAtras);
        long rejeitadas30Dias = solicitacaoRepository.countByStatusAndAtualizadoEmAfter(
                StatusSolicitation.REJEITADA, trintaDiasAtras);

        return new DashboardStatsDto(
                solicitacoesPendentes,
                totalFuncionarios,
                aprovadas30Dias,
                rejeitadas30Dias
        );
    }
}