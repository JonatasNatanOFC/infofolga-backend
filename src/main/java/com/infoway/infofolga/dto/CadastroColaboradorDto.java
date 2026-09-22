package com.infoway.infofolga.dto;

import com.infoway.infofolga.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CadastroColaboradorDto(
        @NotBlank(message = "O nome é obrigatório") @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres") String nome,

        @NotBlank(message = "O CPF é obrigatório") String cpf,

        @NotBlank(message = "O e-mail é obrigatório") @Email(message = "Formato de e-mail inválido") String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String senha,

        @Size(max = 100, message = "O cargo deve ter no máximo 100 caracteres") String cargo,
        @Size(max = 100, message = "O setor deve ter no máximo 100 caracteres") String setor,
        String foto,

        @NotNull(message = "O nível de acesso (Role) é obrigatório") Role role) {
}