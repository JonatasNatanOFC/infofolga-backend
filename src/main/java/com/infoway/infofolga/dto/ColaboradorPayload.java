package com.infoway.infofolga.dto;

public record ColaboradorPayload(
        String nome,
        String cpf,
        String senha,
        String matricula,
        String cargo,
        String setor,
        String foto,
        String status
) {
}