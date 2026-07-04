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

                Long gerenteId,
                String gerenteNome,
                String gerenteFoto,

                TipoSolicitacao tipo,
                StatusSolicitation status,
                LocalDate dataInicio,
                LocalDate dataFim,
                String motivo,
                String motivoResposta,
                LocalDateTime criadoEm) {
        public SolicitacaoDto(Solicitacao s) {
                this(
                                s.getId(),

                                s.getFuncionario() != null ? s.getFuncionario().getId() : null,

                                s.getFuncionario() != null ? s.getFuncionario().getNome()
                                                : (s.getNomeHistorico() != null ? s.getNomeHistorico()
                                                                : "Desconhecido"),
                                s.getFuncionario() != null ? s.getFuncionario().getFoto() : s.getFotoHistorico(),
                                s.getFuncionario() != null ? s.getFuncionario().getSetor() : s.getSetorHistorico(),
                                s.getFuncionario() != null ? s.getFuncionario().getCargo()
                                                : (s.getCargoHistorico() != null ? s.getCargoHistorico()
                                                                : "Cargo não informado"),

                                s.getGerente() != null ? s.getGerente().getId() : null,
                                s.getGerente() != null ? s.getGerente().getNome() : null,
                                s.getGerente() != null ? s.getGerente().getFoto() : null,

                                s.getTipo(),
                                s.getStatus(),
                                s.getDataInicio(),
                                s.getDataFim(),
                                s.getMotivo(),
                                s.getMotivoResposta(),
                                s.getCriadoEm());
        }
}