package com.infoway.infofolga.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Resumo de uma importação de ponto para o RH conferir.
 *
 * @param novas             batidas gravadas agora (as que já existiam são ignoradas)
 * @param formatoAntigo     marcações no leiaute antigo (Portaria 1510, por PIS), que não dá para ligar a ninguém
 * @param cpfsNaoEncontrados CPFs do arquivo sem cadastro nesta empresa, mascarados
 * @param diasCalculados    dias de banco de horas recalculados
 */
public record ResultadoImportacaoDto(int linhas, int marcacoes, int novas, int formatoAntigo,
                                     List<String> cpfsNaoEncontrados, LocalDate inicio, LocalDate fim,
                                     int diasCalculados) {
}
