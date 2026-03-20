package com.infoway.infofolga.dto;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Role;

public record UsuarioDto(Long id, String nome, String matricula, String setor, String cargo, Role role, String cpf, String foto, String status) {
    public UsuarioDto(Funcionario funcionario) {
        this(
                funcionario.getId(),
                funcionario.getNome(),
                funcionario.getMatricula(),
                funcionario.getSetor(),
                funcionario.getCargo(),
                funcionario.getRole(),
                funcionario.getCpf(),
                funcionario.getFoto(),
                funcionario.getStatus()
        );
    }
}