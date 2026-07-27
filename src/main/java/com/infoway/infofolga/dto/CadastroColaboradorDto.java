package com.infoway.infofolga.dto;

import com.infoway.infofolga.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CadastroColaboradorDto(
        @NotBlank(message = "O nome é obrigatório") String nome,

        @NotBlank(message = "O CPF é obrigatório") String cpf,

        @NotBlank(message = "O e-mail é obrigatório") @Email(message = "Formato de e-mail inválido") String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min=8, message = "A senha deve ter no mínimo 8 caracteres")
        String senha,

        String cargo,
        String setor,
        String foto,

        @NotNull(message = "O nível de acesso (Role) é obrigatório") Role role) {
}