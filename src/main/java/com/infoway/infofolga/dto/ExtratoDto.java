package com.infoway.infofolga.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Extrato de pontos de um colaborador em um mês.
 *
 * @param saldo       saldo acumulado de todos os meses
 * @param saldoDoMes  soma só dos dias do mês pedido
 * @param mes         "AAAA-MM"
 * @param dias        do mais recente para o mais antigo
 */
public record ExtratoDto(ColaboradorResumoDto colaborador, BigDecimal saldo, BigDecimal saldoDoMes, String mes,
                         List<DiaExtratoDto> dias) {
}
