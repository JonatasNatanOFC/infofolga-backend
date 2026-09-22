package com.infoway.infofolga.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejeitarSolicitacaoDto(
                @NotBlank(message = "O motivo da rejeição é obrigatório") @Size(max = 1000, message = "O motivo deve ter no máximo 1000 caracteres") String motivoResposta) {
}