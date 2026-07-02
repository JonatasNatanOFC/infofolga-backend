package com.infoway.infofolga.dto;

public record FuncionarioPayload(
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