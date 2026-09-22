package com.infoway.infofolga.dto;

import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.util.FotoUtils;

/**
 * Dados públicos de um colaborador exibidos junto às solicitações (sem CPF, e-mail ou nível de acesso).
 */
public record ColaboradorResumoDto(
        Long id,
        String nome,
        String cargo,
        String setor,
        String foto) {
    public ColaboradorResumoDto(Colaborador colaborador) {
        this(colaborador.getId(), colaborador.getNome(), colaborador.getCargo(),
                colaborador.getSetor(),
                FotoUtils.url(colaborador.getFoto(), "/api/colaboradores/" + colaborador.getId() + "/foto"));
    }
}
