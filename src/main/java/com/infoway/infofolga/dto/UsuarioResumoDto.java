package com.infoway.infofolga.dto;

import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.model.Role;

public record UsuarioResumoDto(
        Long id,
        String nome,
        String email,
        String cargo,
        String setor,
        String cpf,
        Role role,
        String status,
        String foto
) {
    public UsuarioResumoDto(Colaborador c) {
        this(
                c.getId(),
                c.getNome(),
                c.getEmail(),
                c.getCargo(),
                c.getSetor(),
                c.getCpf(),
                c.getRole(),
                c.getStatus(),
                c.getFoto()
        );
    }
}