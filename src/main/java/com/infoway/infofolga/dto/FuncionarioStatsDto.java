package com.infoway.infofolga.dto;

public record FuncionarioStatsDto(
        long solicitacoesPendentes,
        long solicitacoesAprovadas,
        long solicitacoesRejeitadas,
        long diasDeFolgaUsados
) {}
