package com.infoway.infofolga.dto;

import com.infoway.infofolga.model.Role; // Importe o Enum Role

/**
 * DTO para a resposta do login.
 * A MUDANÇA PRINCIPAL É A ADIÇÃO DO CAMPO 'role'.
 */
public record LoginResponseDto(String token, String nomeUsuario, Role role) {
}