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
        Role role,
        // Só preenchido no /me: o app usa para mostrar saldo e a opção de folga do banco de horas.
        Boolean temBancoDeHoras) {
    public UsuarioDto(Colaborador colaborador) {
        this(colaborador.getId(), colaborador.getNome(), colaborador.getCpf(), colaborador.getEmail(),
                colaborador.getCargo(), colaborador.getSetor(),
                FotoUtils.url(colaborador.getFoto(), "/api/colaboradores/" + colaborador.getId() + "/foto"),
                colaborador.getStatus(),
                colaborador.getRole(), null);
    }

    public UsuarioDto comBancoDeHoras(boolean tem) {
        return new UsuarioDto(id, nome, cpf, email, cargo, setor, foto, status, role, tem);
    }
}