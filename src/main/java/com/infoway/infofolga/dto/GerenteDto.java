package com.infoway.infofolga.dto;

import com.infoway.infofolga.model.Gerente;

public record GerenteDto(Long id, String nome, String cpf, String status) {
    public GerenteDto(Gerente gerente) {
        this(gerente.getId(), gerente.getNome(), gerente.getCpf(), gerente.getStatus());
    }
}