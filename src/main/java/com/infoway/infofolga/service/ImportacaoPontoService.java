package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.ResultadoImportacaoDto;
import com.infoway.infofolga.model.Batida;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.ponto.LeitorAfd;
import com.infoway.infofolga.ponto.Marcacao;
import com.infoway.infofolga.repository.BatidaRepository;
import com.infoway.infofolga.repository.ColaboradorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Entrada das batidas no InfoFolga, venham de onde vierem. O AFD usa isto hoje; o cliente da API do Faceponto
 * (ou de outro fornecedor) vai chamar {@link #registrar} com as marcações que buscar.
 */
@Service
public class ImportacaoPontoService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final BatidaRepository batidas;
    private final ColaboradorRepository colaboradores;
    private final ApuracaoService apuracao;

    public ImportacaoPontoService(BatidaRepository batidas, ColaboradorRepository colaboradores,
                                  ApuracaoService apuracao) {
        this.batidas = batidas;
        this.colaboradores = colaboradores;
        this.apuracao = apuracao;
    }

    @Transactional
    public ResultadoImportacaoDto importarAfd(InputStream arquivo) {
        LeitorAfd.Resultado lido;
        try {
            // O AFD é texto ASCII; ISO-8859-1 lê qualquer byte sem falhar.
            lido = LeitorAfd.ler(new InputStreamReader(arquivo, StandardCharsets.ISO_8859_1));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo.");
        }
        if (lido.marcacoes().isEmpty()) {
            String motivo = lido.formatoAntigo() > 0
                    ? "O arquivo está no formato antigo (Portaria 1510, por PIS). Exporte o AFD no formato da Portaria 671, que usa o CPF."
                    : "Nenhuma marcação de ponto encontrada. Confira se é o arquivo AFD do relógio.";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, motivo);
        }
        return registrar(lido.marcacoes(), Batida.Origem.AFD, lido.linhas(), lido.formatoAntigo());
    }

    /** Grava as batidas novas da empresa do usuário logado e recalcula os dias de quem recebeu batidas. */
    @Transactional
    public ResultadoImportacaoDto registrar(List<Marcacao> marcacoes, Batida.Origem origem, int linhas,
                                            int formatoAntigo) {
        LocalDate hoje = LocalDate.now(FUSO);
        Map<String, List<Marcacao>> porCpf = marcacoes.stream()
                .collect(Collectors.groupingBy(Marcacao::cpf, TreeMap::new, Collectors.toList()));

        int novas = 0;
        int dias = 0;
        List<String> naoEncontrados = new ArrayList<>();
        for (var entrada : porCpf.entrySet()) {
            // O @TenantId limita a busca à empresa de quem importa: CPF de outra empresa conta como não encontrado.
            Optional<Colaborador> colaborador = colaboradores.findByCpf(entrada.getKey());
            if (colaborador.isEmpty()) {
                naoEncontrados.add(mascarar(entrada.getKey()));
                continue;
            }
            List<LocalDateTime> horarios = entrada.getValue().stream().map(Marcacao::dataHora).sorted().toList();
            novas += gravarNovas(colaborador.get().getId(), horarios, origem);

            LocalDate inicio = horarios.get(0).toLocalDate();
            LocalDate fim = horarios.get(horarios.size() - 1).toLocalDate();
            // O dia de hoje ainda está acontecendo: fica para a próxima importação.
            if (!inicio.isAfter(hoje.minusDays(1))) {
                dias += apuracao.reprocessar(colaborador.get(), inicio, fim.isBefore(hoje) ? fim : hoje.minusDays(1));
            }
        }
        LocalDateTime primeira = marcacoes.stream().map(Marcacao::dataHora).min(Comparator.naturalOrder()).orElseThrow();
        LocalDateTime ultima = marcacoes.stream().map(Marcacao::dataHora).max(Comparator.naturalOrder()).orElseThrow();
        return new ResultadoImportacaoDto(linhas, marcacoes.size(), novas, formatoAntigo, naoEncontrados,
                primeira.toLocalDate(), ultima.toLocalDate(), dias);
    }

    private int gravarNovas(Long colaboradorId, List<LocalDateTime> horarios, Batida.Origem origem) {
        Set<LocalDateTime> existentes = new HashSet<>();
        batidas.findByColaboradorIdAndDataHoraBetweenOrderByDataHora(
                        colaboradorId, horarios.get(0), horarios.get(horarios.size() - 1))
                .forEach(b -> existentes.add(b.getDataHora()));
        List<Batida> novas = horarios.stream()
                .filter(h -> !existentes.contains(h))
                .map(h -> new Batida(colaboradorId, h, origem))
                .toList();
        batidas.saveAll(novas);
        return novas.size();
    }

    /** "52998224725" -> "529.***.***-25": o RH reconhece a pessoa sem o CPF inteiro aparecer na tela. */
    static String mascarar(String cpf) {
        return cpf.substring(0, 3) + ".***.***-" + cpf.substring(9);
    }
}
