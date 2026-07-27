package com.infoway.infofolga.dto;

import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.model.Role;

public record UsuarioDto(
        Long id,
        String nome,
        String cpf,
        String email,
        String cargo,
        String setor,
        String foto,
        String status,
        Role role) {
    public UsuarioDto(Colaborador colaborador) {
        this(colaborador.getId(), colaborador.getNome(), colaborador.getCpf(), colaborador.getEmail(),
                colaborador.getCargo(), colaborador.getSetor(), colaborador.getFoto(),
                colaborador.getStatus(),
                colaborador.getRole());
    }
}