package com.infoway.infofolga.pontuacao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Um dia de um colaborador, pronto para apurar.
 *
 * @param data     dia a que o turno pertence
 * @param batidas  marcações do ponto. Um turno que passa da meia-noite fica no dia em que começou,
 *                 então uma batida pode ter a data do dia seguinte
 * @param situacao o que aconteceu no dia (normal, feriado, folga...)
 */
public record DiaTrabalho(LocalDate data, List<LocalDateTime> batidas, SituacaoDia situacao) {

    public DiaTrabalho {
        Objects.requireNonNull(data);
        Objects.requireNonNull(situacao);
        batidas = batidas == null ? List.of() : List.copyOf(batidas);
    }
}
