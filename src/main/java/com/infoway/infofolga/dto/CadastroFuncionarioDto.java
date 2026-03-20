package com.infoway.infofolga.dto;

public record CadastroFuncionarioDto(
        String nome,
        String matricula,
        String cargo,
        String setor,
        String cpf,
        String senha,
        String foto,
        String status
) {}