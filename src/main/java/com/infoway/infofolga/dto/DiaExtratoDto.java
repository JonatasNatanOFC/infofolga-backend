package com.infoway.infofolga.dto;

import com.infoway.infofolga.model.ApuracaoDia;
import com.infoway.infofolga.pontuacao.SituacaoDia;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Uma linha do extrato: o dia, o que aconteceu e quantos pontos deu. */
public record DiaExtratoDto(LocalDate data, SituacaoDia situacao, BigDecimal pontos, boolean inconsistente,
                            String descricao) {

    public DiaExtratoDto(ApuracaoDia dia) {
        this(dia.getData(), dia.getSituacao(), dia.getPontos(), dia.isInconsistente(), dia.getDescricao());
    }
}
