package com.infoway.infofolga.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
        @NotBlank(message = "CPF é obrigatório")
        @Size(max = 14, message = "CPF inválido")
        String cpf,

        @NotBlank(message = "Senha é obrigatória")
        @Size(max = 72, message = "Usuário ou senha inválidos.")
        String senha
) {}