package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.DashboardStatsDto;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.repository.ColaboradorRepository;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardService {

        private final SolicitacaoRepository solicitacaoRepository;
        private final ColaboradorRepository colaboradorRepository;

        public DashboardService(SolicitacaoRepository solicitacaoRepository, ColaboradorRepository colaboradorRepository) {
                this.solicitacaoRepository = solicitacaoRepository;
                this.colaboradorRepository = colaboradorRepository;
        }

        @Cacheable(value = "dashboard_stats")
        public DashboardStatsDto getStats() {
                long totalColaboradores = colaboradorRepository.count();
                long totalSolicitacoes = solicitacaoRepository.count();

                LocalDateTime trintaDiasAtras = LocalDateTime.now().minusDays(30);
                LocalDate hoje = LocalDate.now();
                LocalDate daquiA7Dias = hoje.plusDays(7);

                long aprovadas = solicitacaoRepository.countByStatusAndAtualizadoEmAfter(StatusSolicitation.APROVADA, trintaDiasAtras);
                long rejeitadas = solicitacaoRepository.countByStatusAndAtualizadoEmAfter(StatusSolicitation.REJEITADA, trintaDiasAtras);
                long pendentes = solicitacaoRepository.countByStatusAndAtualizadoEmAfter(StatusSolicitation.PENDENTE, trintaDiasAtras);
                long naoUsufruidas = solicitacaoRepository.countByStatusAndAtualizadoEmAfter(StatusSolicitation.INVALIDADA, trintaDiasAtras);

                List<StatusSolicitation> statusAtivos = List.of(StatusSolicitation.APROVADA, StatusSolicitation.USUFRUIDA);
                long folgasHoje = solicitacaoRepository.countFolgasAtivasHoje(statusAtivos, hoje);
                long proximasFolgas = solicitacaoRepository.countFolgasProximosDias(StatusSolicitation.APROVADA, hoje, daquiA7Dias);

                return new DashboardStatsDto(
                        totalColaboradores,
                        totalSolicitacoes,
                        aprovadas,
                        rejeitadas,
                        pendentes,
                        naoUsufruidas,
                        folgasHoje,
                        proximasFolgas
                );
        }
}