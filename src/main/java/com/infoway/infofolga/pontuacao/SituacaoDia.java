package com.infoway.infofolga.pontuacao;

/** O que aconteceu no dia, do ponto de vista do banco de horas. */
public enum SituacaoDia {
    /** Dia comum: vale a jornada prevista para o dia da semana. */
    NORMAL("Dia normal"),
    /** Feriado ou folga coletiva do calendário da empresa: nada previsto, hora trabalhada vale com o peso de feriado. */
    FERIADO("Feriado"),
    /** Folga para compensar horas: a jornada do dia sai do saldo. */
    FOLGA_COM_DESCONTO("Folga (banco de horas)"),
    /** Day off de aniversário ou folga dada pela empresa: não mexe no saldo. */
    FOLGA_SEM_DESCONTO("Folga sem desconto"),
    FERIAS("Férias"),
    /** Atestado e faltas justificadas (art. 473 da CLT): não mexe no saldo. */
    FALTA_JUSTIFICADA("Falta justificada"),
    /** Descontada em folha pela CLT; só entra no saldo se a empresa optar por compensar no banco de horas. */
    FALTA_INJUSTIFICADA("Falta sem justificativa");

    private final String rotulo;

    SituacaoDia(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
