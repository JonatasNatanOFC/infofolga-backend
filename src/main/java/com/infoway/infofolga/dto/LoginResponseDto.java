package com.infoway.infofolga.dto;

import com.infoway.infofolga.model.Role;

public record LoginResponseDto(String token, String nomeUsuario, Role role) {
}