package com.infoway.infofolga.dto;

import jakarta.validation.constraints.NotBlank;

public record CadastroFuncionarioDto(
        @NotBlank(message = "Nome é obrigatório")
        String nome,
        String matricula,
        String cargo,
        String setor,
        String cpf,
        String senha,
        String foto,
        String status
) {}