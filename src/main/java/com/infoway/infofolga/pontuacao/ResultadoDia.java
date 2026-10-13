package com.infoway.infofolga.pontuacao;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resultado de um dia: o que vai para a tabela de apuração e para o extrato do colaborador.
 *
 * @param segundosTrabalhados tempo trabalhado já com a hora noturna convertida
 * @param pontos              saldo do dia em horas, com duas casas (positivo soma, negativo desconta)
 * @param inconsistente       true quando falta uma batida: o dia fica sem pontos até o RH corrigir o ponto
 * @param descricao           texto do extrato
 */
public record ResultadoDia(
        LocalDate data,
        SituacaoDia situacao,
        long segundosTrabalhados,
        long segundosPrevistos,
        BigDecimal pontos,
        boolean inconsistente,
        String descricao) {
}
