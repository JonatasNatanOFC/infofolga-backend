package com.infoway.infofolga.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Um feriado do calendário. Os nacionais vêm calculados (sem id, não podem ser removidos);
 * os da empresa têm id. No envio, só data, descrição e anual contam.
 * Um feriado anual vem com a data do ano consultado; remover apaga todas as ocorrências.
 */
public record FeriadoDto(
        Long id,
        @NotNull(message = "Informe a data do feriado.") LocalDate data,
        @NotBlank(message = "Informe o nome do feriado.") @Size(max = 120, message = "Nome com no máximo 120 caracteres.") String descricao,
        boolean anual,
        boolean nacional) {
}
