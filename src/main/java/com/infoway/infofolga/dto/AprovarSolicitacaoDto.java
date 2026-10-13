package com.infoway.infofolga.dto;

/**
 * Corpo opcional da aprovação. Para folgas, o gerente confirma se ela desconta do saldo de horas;
 * sem corpo (ou null), vale o padrão do motivo escolhido no pedido.
 */
public record AprovarSolicitacaoDto(Boolean descontarSaldo) {
}
