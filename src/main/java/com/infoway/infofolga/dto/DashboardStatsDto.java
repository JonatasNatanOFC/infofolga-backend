package com.infoway.infofolga.dto;

public record DashboardStatsDto(
        long totalColaboradores,
        long totalSolicitacoes,
        long aprovadas30Dias,
        long rejeitadas30Dias,
        long pendentes30Dias,
        long naoUsufruidas30Dias,
        long folgasHoje,
        long proximasFolgas
) {
}