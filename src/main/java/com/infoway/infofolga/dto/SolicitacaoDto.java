package com.infoway.infofolga.dto;

import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.util.FotoUtils;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.model.TipoSolicitacao;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SolicitacaoDto(
                Long id,
                String nomeHistorico,
                String cargoHistorico,
                String setorHistorico,
                String fotoHistorico,
                TipoSolicitacao tipo,
                StatusSolicitation status,
                LocalDate dataInicio,
                LocalDate dataFim,
                String motivo,
                String motivoResposta,
                LocalDateTime criadoEm,
                LocalDateTime atualizadoEm,
                ColaboradorResumoDto colaborador,
                ColaboradorResumoDto aprovador) {
        public SolicitacaoDto(Solicitacao solicitacao) {
                this(
                                solicitacao.getId(),
                                solicitacao.getNomeHistorico(),
                                solicitacao.getCargoHistorico(),
                                solicitacao.getSetorHistorico(),
                                FotoUtils.url(solicitacao.getFotoHistorico(), "/api/solicitacoes/" + solicitacao.getId() + "/foto"),
                                solicitacao.getTipo(),
                                solicitacao.getStatus(),
                                solicitacao.getDataInicio(),
                                solicitacao.getDataFim(),
                                solicitacao.getMotivo(),
                                solicitacao.getMotivoResposta(),
                                solicitacao.getCriadoEm(),
                                solicitacao.getAtualizadoEm(),
                                solicitacao.getColaborador() != null ? new ColaboradorResumoDto(solicitacao.getColaborador())
                                                : null,
                                solicitacao.getAprovador() != null ? new ColaboradorResumoDto(solicitacao.getAprovador()) : null);
        }
}