package com.infoway.infofolga.dto;

import jakarta.validation.constraints.NotBlank;

public record RejeitarSolicitacaoDto(
                @NotBlank(message = "O motivo da rejeição é obrigatório") String motivoResposta) {
}