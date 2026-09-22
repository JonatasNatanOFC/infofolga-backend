package com.infoway.infofolga.dto;

import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.model.Role;
import com.infoway.infofolga.util.FotoUtils;

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
                colaborador.getCargo(), colaborador.getSetor(),
                FotoUtils.url(colaborador.getFoto(), "/api/colaboradores/" + colaborador.getId() + "/foto"),
                colaborador.getStatus(),
                colaborador.getRole());
    }
}