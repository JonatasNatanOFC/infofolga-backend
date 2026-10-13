package com.infoway.infofolga.pontuacao;

/** O que fazer quando alguém pede folga de banco de horas sem saldo suficiente. Escolha de cada empresa. */
public enum PoliticaSemSaldo {
    /** O saldo pode ficar negativo. */
    PERMITIR,
    /** O pedido passa, mas o gerente vê o alerta antes de aprovar. */
    AVISAR,
    /** O app não deixa pedir. */
    BLOQUEAR
}
