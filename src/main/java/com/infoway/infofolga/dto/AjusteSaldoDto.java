package com.infoway.infofolga.dto;

import com.infoway.infofolga.model.AjusteSaldo;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Ajuste manual do banco de horas. No envio, só data, pontos e justificativa contam. */
public record AjusteSaldoDto(
        Long id,
        @NotNull(message = "Informe a data do ajuste.") LocalDate data,
        @NotNull(message = "Informe os pontos.")
        @DecimalMin(value = "-999.99", message = "Ajuste entre -999,99 e 999,99.")
        @DecimalMax(value = "999.99", message = "Ajuste entre -999,99 e 999,99.")
        @Digits(integer = 3, fraction = 2, message = "Use no máximo duas casas decimais.") BigDecimal pontos,
        @NotBlank(message = "Explique o motivo do ajuste.") @Size(max = 255, message = "Motivo com no máximo 255 caracteres.") String justificativa,
        String lancadoPor,
        LocalDateTime criadoEm) {

    public AjusteSaldoDto(AjusteSaldo ajuste, String nomeDeQuemLancou) {
        this(ajuste.getId(), ajuste.getData(), ajuste.getPontos(), ajuste.getJustificativa(), nomeDeQuemLancou,
                ajuste.getCriadoEm());
    }
}
