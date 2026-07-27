package com.infoway.infofolga.dto;

public record ColaboradorStatsDto(
        long solicitacoesPendentes,
        long solicitacoesAprovadas,
        long solicitacoesRejeitadas,
        long diasDeFolgaUsados
) {}
