package com.infoway.infofolga.dto;

import java.math.BigDecimal;

/** Linha da lista da equipe: saldo atual e quantos dias esperam correção do ponto. */
public record SaldoColaboradorDto(ColaboradorResumoDto colaborador, BigDecimal saldo, long diasPendentes) {
}
