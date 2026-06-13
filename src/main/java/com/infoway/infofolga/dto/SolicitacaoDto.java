package com.infoway.infofolga.dto;

import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.model.TipoSolicitacao;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SolicitacaoDto(
        Long id,
        Long funcionarioId,
        String funcionarioNome,
        String funcionarioFoto,
        String funcionarioSetor,
        String funcionarioCargo,
        TipoSolicitacao tipo,
        StatusSolicitation status,
        LocalDate dataInicio,
        LocalDate dataFim,
        String motivo,
        String motivoResposta,
        LocalDateTime criadoEm
) {
    public SolicitacaoDto(Solicitacao s) {
        this(
                s.getId(),
                s.getFuncionario().getId(),
                s.getFuncionario().getNome(),
                s.getFuncionario().getFoto(),
                s.getFuncionario().getSetor(),
                s.getFuncionario().getCargo(),
                s.getTipo(),
                s.getStatus(),
                s.getDataInicio(),
                s.getDataFim(),
                s.getMotivo(),
                s.getMotivoResposta(),
                s.getCriadoEm()
        );
    }
}