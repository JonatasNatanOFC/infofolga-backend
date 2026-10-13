package com.infoway.infofolga.model;

/** Por que o colaborador pediu a folga. Define se ela desconta do saldo de horas por padrão. */
public enum MotivoFolga {
    BANCO_DE_HORAS(true),
    ANIVERSARIO(false),
    OUTRO(true);

    private final boolean descontaPorPadrao;

    MotivoFolga(boolean descontaPorPadrao) {
        this.descontaPorPadrao = descontaPorPadrao;
    }

    /** Sugestão para o gerente; quem decide é ele, ao aprovar. */
    public boolean descontaPorPadrao() {
        return descontaPorPadrao;
    }
}
