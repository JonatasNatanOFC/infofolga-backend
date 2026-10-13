package com.infoway.infofolga.ponto;

import java.time.LocalDateTime;
import java.util.Objects;

/** Uma batida de ponto como vem da fonte (AFD, API do fornecedor): CPF com 11 dígitos e horário local. */
public record Marcacao(String cpf, LocalDateTime dataHora) {

    public Marcacao {
        Objects.requireNonNull(cpf);
        Objects.requireNonNull(dataHora);
    }
}
