package com.infoway.infofolga.util;

public class CpfUtils {

    private CpfUtils() {
    }

    public static String limpar(String cpf) {
        if (cpf == null) {
            return null;
        }

        return cpf.replaceAll("\\D", "");
    }

    public static boolean isValido(String cpf) {
        String numeros = limpar(cpf);
        if (numeros == null || numeros.length() != 11 || numeros.chars().distinct().count() == 1) {
            return false;
        }

        for (int posicao = 9; posicao <= 10; posicao++) {
            int soma = 0;
            for (int i = 0; i < posicao; i++) {
                soma += (numeros.charAt(i) - '0') * (posicao + 1 - i);
            }
            int digito = (soma * 10) % 11 % 10;
            if (digito != numeros.charAt(posicao) - '0') {
                return false;
            }
        }
        return true;
    }

    public static String formatar(String cpf) {
        if (cpf == null) {
            return null;
        }

        String numeros = limpar(cpf);

        if (numeros.length() != 11) {
            return cpf;
        }

        return numeros.replaceAll(
                "^(\\d{3})(\\d{3})(\\d{3})(\\d{2})$",
                "$1.$2.$3-$4"
        );
    }
}