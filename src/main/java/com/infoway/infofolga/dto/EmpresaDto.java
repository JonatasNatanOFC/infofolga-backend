package com.infoway.infofolga.dto;

import com.infoway.infofolga.model.Empresa;

public record EmpresaDto(Long id, String nome, String logo, String logoRelatorio) {
    public EmpresaDto(Empresa empresa) {
        this(empresa.getId(), empresa.getNome(), empresa.getLogo(), empresa.getLogoRelatorio());
    }
}
