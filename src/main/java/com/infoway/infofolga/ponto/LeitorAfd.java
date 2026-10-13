package com.infoway.infofolga.ponto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lê as marcações do Arquivo-Fonte de Dados (AFD) da Portaria MTP 671/2021, que todo relógio de ponto exporta.
 * Registros de marcação: tipo 3 (REP-C e REP-A) e tipo 7 (REP-P), com o mesmo começo:
 * NSR (posições 1-9), tipo (10), data e hora "AAAA-MM-ddThh:mm:00-0300" (11-34) e CPF (35-46).
 * Os demais registros (cabeçalho, empresa, ajustes de relógio, trailer, assinatura) são ignorados.
 */
public final class LeitorAfd {

    /** O que foi lido. formatoAntigo conta marcações no leiaute da Portaria 1510, que identifica pelo PIS. */
    public record Resultado(List<Marcacao> marcacoes, int linhas, int formatoAntigo) {
    }

    private LeitorAfd() {
    }

    public static Resultado ler(Reader arquivo) throws IOException {
        // LinkedHashSet: a mesma batida repetida no arquivo conta uma vez só.
        Set<Marcacao> marcacoes = new LinkedHashSet<>();
        int linhas = 0;
        int formatoAntigo = 0;
        try (BufferedReader leitor = new BufferedReader(arquivo)) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                linhas++;
                if (linha.length() < 34 || !nsrValido(linha)) {
                    continue;
                }
                char tipo = linha.charAt(9);
                if (tipo == '3' && linha.trim().length() == 34) {
                    formatoAntigo++; // Portaria 1510: DDMMAAAA + HHMM + PIS
                    continue;
                }
                if ((tipo == '3' || tipo == '7') && linha.length() >= 46) {
                    Marcacao marcacao = marcacao(linha);
                    if (marcacao != null) {
                        marcacoes.add(marcacao);
                    }
                }
            }
        }
        return new Resultado(new ArrayList<>(marcacoes), linhas, formatoAntigo);
    }

    private static boolean nsrValido(String linha) {
        for (int i = 0; i < 9; i++) {
            if (!Character.isDigit(linha.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static Marcacao marcacao(String linha) {
        String cpf = linha.substring(34, 46).replaceAll("\\D", "");
        if (cpf.length() == 12 && cpf.charAt(0) == '0') {
            cpf = cpf.substring(1);
        }
        if (cpf.length() != 11) {
            return null;
        }
        try {
            // shortcut: o fuso (-0300) é descartado e vale o horário do relógio; empresa com filial em outro fuso
            // precisaria converter para o fuso da empresa.
            return new Marcacao(cpf, LocalDateTime.parse(linha.substring(10, 29)));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
