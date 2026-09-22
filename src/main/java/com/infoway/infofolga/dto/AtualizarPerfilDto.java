package com.infoway.infofolga.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Dados que o próprio colaborador pode alterar no seu perfil.
 * Nome, cargo, setor, CPF e nível de acesso só podem ser alterados pelo CEO.
 */
public record AtualizarPerfilDto(
        @Email(message = "Formato de e-mail inválido") String email,
        String foto,
        String senhaAtual,
        @Size(min = 8, max = 72, message = "A nova senha deve ter entre 8 e 72 caracteres") String novaSenha) {
}
