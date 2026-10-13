package com.infoway.infofolga.dto;

import com.infoway.infofolga.pontuacao.ExemplosPontuacao;

import java.math.BigDecimal;

/** Um dia de exemplo da simulação: o que aconteceu, o texto do extrato e os pontos. */
public record ExemploPontuacaoDto(String titulo, String descricao, BigDecimal pontos) {

    public ExemploPontuacaoDto(ExemplosPontuacao.Exemplo exemplo) {
        this(exemplo.titulo(), exemplo.resultado().descricao(), exemplo.resultado().pontos());
    }
}
