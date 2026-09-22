package com.infoway.infofolga.dto;

import com.infoway.infofolga.model.TipoSolicitacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CriarSolicitacaoDto(
        @NotNull(message = "O tipo da solicitação é obrigatório.")
        TipoSolicitacao tipo,

        @NotNull(message = "A data inicial é obrigatória.")
        LocalDate dataInicio,

        @NotNull(message = "A data final é obrigatória.")
        LocalDate dataFim,

        @Size(max = 1000, message = "O motivo deve ter no máximo 1000 caracteres.")
        String motivo
) {}