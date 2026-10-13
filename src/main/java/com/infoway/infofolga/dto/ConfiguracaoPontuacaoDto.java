package com.infoway.infofolga.dto;

import com.infoway.infofolga.model.ConfiguracaoPontuacao;
import com.infoway.infofolga.pontuacao.PoliticaSemSaldo;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Uma versão das regras de pontuação. Na leitura, traz quem criou e quando; no envio, esses campos são ignorados.
 * Jornadas em minutos.
 */
public record ConfiguracaoPontuacaoDto(
        Long id,
        @NotNull(message = "Informe a partir de quando as regras valem.") LocalDate vigenteDesde,
        @NotNull @Min(value = 0, message = "Jornada inválida.") @Max(value = 1440, message = "Jornada inválida.") Integer jornadaSeg,
        @NotNull @Min(value = 0, message = "Jornada inválida.") @Max(value = 1440, message = "Jornada inválida.") Integer jornadaTer,
        @NotNull @Min(value = 0, message = "Jornada inválida.") @Max(value = 1440, message = "Jornada inválida.") Integer jornadaQua,
        @NotNull @Min(value = 0, message = "Jornada inválida.") @Max(value = 1440, message = "Jornada inválida.") Integer jornadaQui,
        @NotNull @Min(value = 0, message = "Jornada inválida.") @Max(value = 1440, message = "Jornada inválida.") Integer jornadaSex,
        @NotNull @Min(value = 0, message = "Jornada inválida.") @Max(value = 1440, message = "Jornada inválida.") Integer jornadaSab,
        @NotNull @Min(value = 0, message = "Jornada inválida.") @Max(value = 1440, message = "Jornada inválida.") Integer jornadaDom,
        @NotNull @Min(value = 0, message = "Tolerância entre 0 e 60 minutos.") @Max(value = 60, message = "Tolerância entre 0 e 60 minutos.") Integer toleranciaMin,
        @NotNull @DecimalMin(value = "0.5", message = "Peso entre 0,5 e 5.") @DecimalMax(value = "5", message = "Peso entre 0,5 e 5.") BigDecimal pesoDomingo,
        @NotNull @DecimalMin(value = "0.5", message = "Peso entre 0,5 e 5.") @DecimalMax(value = "5", message = "Peso entre 0,5 e 5.") BigDecimal pesoFeriado,
        @NotNull @DecimalMin(value = "1", message = "Fator noturno entre 1 e 2.") @DecimalMax(value = "2", message = "Fator noturno entre 1 e 2.") BigDecimal fatorHoraNoturna,
        @NotNull Boolean compensarFalta,
        @NotNull(message = "Escolha a regra para folga sem saldo.") PoliticaSemSaldo politicaSemSaldo,
        @Min(value = 1, message = "Validade entre 1 e 24 meses.") @Max(value = 24, message = "Validade entre 1 e 24 meses.") Integer validadeMeses,
        String alteradoPor,
        LocalDateTime criadoEm) {

    public ConfiguracaoPontuacaoDto(ConfiguracaoPontuacao c, String nomeDeQuemAlterou) {
        this(c.getId(), c.getVigenteDesde(), c.getJornadaSeg(), c.getJornadaTer(), c.getJornadaQua(), c.getJornadaQui(),
                c.getJornadaSex(), c.getJornadaSab(), c.getJornadaDom(), c.getToleranciaMin(), c.getPesoDomingo(),
                c.getPesoFeriado(), c.getFatorHoraNoturna(), c.isCompensarFalta(), c.getPoliticaSemSaldo(),
                c.getValidadeMeses(), nomeDeQuemAlterou, c.getCriadoEm());
    }

    /** Copia as regras para a entidade (não mexe em id, empresa, autor nem data de criação). */
    public void aplicarEm(ConfiguracaoPontuacao c) {
        c.setVigenteDesde(vigenteDesde);
        c.setJornadaSeg(jornadaSeg);
        c.setJornadaTer(jornadaTer);
        c.setJornadaQua(jornadaQua);
        c.setJornadaQui(jornadaQui);
        c.setJornadaSex(jornadaSex);
        c.setJornadaSab(jornadaSab);
        c.setJornadaDom(jornadaDom);
        c.setToleranciaMin(toleranciaMin);
        c.setPesoDomingo(pesoDomingo);
        c.setPesoFeriado(pesoFeriado);
        c.setFatorHoraNoturna(fatorHoraNoturna);
        c.setCompensarFalta(compensarFalta);
        c.setPoliticaSemSaldo(politicaSemSaldo);
        c.setValidadeMeses(validadeMeses);
    }
}
