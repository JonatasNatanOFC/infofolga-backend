package com.infoway.infofolga.util;

public class CpfUtils {

    private CpfUtils() {
    }

    public static String formatar(String cpf) {
        if (cpf == null)
            return null;

        String numeros = cpf.replaceAll("\\D", "");

        if (numeros.length() != 11) {
            return cpf;
        }

        return numeros.replaceAll("^(\\d{3})(\\d{3})(\\d{3})(\\d{2})$", "$1.$2.$3-$4");
    }
}
