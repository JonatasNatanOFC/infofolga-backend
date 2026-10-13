package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.ConfiguracaoPontuacaoDto;
import com.infoway.infofolga.dto.ExemploPontuacaoDto;
import com.infoway.infofolga.dto.FeriadoDto;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.model.ConfiguracaoPontuacao;
import com.infoway.infofolga.model.Feriado;
import com.infoway.infofolga.pontuacao.ExemplosPontuacao;
import com.infoway.infofolga.pontuacao.FeriadosNacionais;
import com.infoway.infofolga.repository.ColaboradorRepository;
import com.infoway.infofolga.repository.ConfiguracaoPontuacaoRepository;
import com.infoway.infofolga.repository.FeriadoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Regras de pontuação e calendário de feriados da empresa do usuário logado (CEO e RH). */
@Service
public class ConfiguracaoPontuacaoService {

    // O "hoje" que importa é o da empresa, não o do servidor (que pode estar em UTC).
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final ConfiguracaoPontuacaoRepository configuracoes;
    private final FeriadoRepository feriados;
    private final ColaboradorRepository colaboradores;

    public ConfiguracaoPontuacaoService(ConfiguracaoPontuacaoRepository configuracoes, FeriadoRepository feriados,
                                        ColaboradorRepository colaboradores) {
        this.configuracoes = configuracoes;
        this.feriados = feriados;
        this.colaboradores = colaboradores;
    }

    /** Todas as versões, da mais nova para a mais antiga. Vazio = pontuação ainda não começou. */
    @Transactional(readOnly = true)
    public List<ConfiguracaoPontuacaoDto> historico() {
        List<ConfiguracaoPontuacao> versoes = configuracoes.findAllByOrderByVigenteDesdeDesc();
        List<Long> autores = versoes.stream().map(ConfiguracaoPontuacao::getAlteradoPor).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> nomes = colaboradores.findAllById(autores).stream()
                .collect(Collectors.toMap(Colaborador::getId, Colaborador::getNome));
        return versoes.stream().map(c -> new ConfiguracaoPontuacaoDto(c, nomes.get(c.getAlteradoPor()))).toList();
    }

    /**
     * Cria a versão que vale a partir da data informada. Se já existir uma versão nessa mesma data (hoje ou futura),
     * ela é substituída: ainda não foi usada em nenhum cálculo.
     */
    @Transactional
    public ConfiguracaoPontuacaoDto salvar(ConfiguracaoPontuacaoDto dto, Colaborador autor) {
        if (dto.vigenteDesde().isBefore(LocalDate.now(FUSO))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "As regras só podem valer a partir de hoje: mudar o passado alteraria pontos já calculados.");
        }
        ConfiguracaoPontuacao versao = configuracoes.findByVigenteDesde(dto.vigenteDesde())
                .orElseGet(ConfiguracaoPontuacao::new);
        dto.aplicarEm(versao);
        versao.setAlteradoPor(autor.getId());
        return new ConfiguracaoPontuacaoDto(configuracoes.save(versao), autor.getNome());
    }

    /** Apura dias de exemplo com as regras do formulário, sem salvar nada. */
    public List<ExemploPontuacaoDto> simular(ConfiguracaoPontuacaoDto dto) {
        ConfiguracaoPontuacao rascunho = new ConfiguracaoPontuacao();
        dto.aplicarEm(rascunho);
        return ExemplosPontuacao.com(rascunho.paraRegras()).stream().map(ExemploPontuacaoDto::new).toList();
    }

    /** Nacionais (calculados) e da empresa, em ordem de data. */
    @Transactional(readOnly = true)
    public List<FeriadoDto> feriadosDoAno(int ano) {
        List<FeriadoDto> lista = new ArrayList<>();
        FeriadosNacionais.doAno(ano).forEach((data, nome) -> lista.add(new FeriadoDto(null, data, nome, false, true)));
        for (Feriado f : feriados.candidatosEntre(LocalDate.of(ano, 1, 1), LocalDate.of(ano, 12, 31))) {
            f.noAno(ano).ifPresent(data -> lista.add(new FeriadoDto(f.getId(), data, f.getDescricao(), f.isAnual(), false)));
        }
        lista.sort(Comparator.comparing(FeriadoDto::data));
        return lista;
    }

    @Transactional
    public FeriadoDto adicionarFeriado(FeriadoDto dto) {
        if (FeriadosNacionais.doAno(dto.data().getYear()).containsKey(dto.data())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta data já é feriado nacional.");
        }
        LocalDate data = dto.data();
        if (dto.anual() && data.getMonthValue() == 2 && data.getDayOfMonth() == 29) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "29 de fevereiro não existe todo ano: cadastre este feriado só para este ano.");
        }
        boolean ocupado = dto.anual()
                ? feriados.conflitaComAnual(data)
                : feriados.existeEm(data);
        if (ocupado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um feriado cadastrado nesta data.");
        }
        Feriado feriado = new Feriado();
        feriado.setData(data);
        feriado.setDescricao(dto.descricao().trim());
        feriado.setAnual(dto.anual());
        Feriado salvo = feriados.save(feriado);
        return new FeriadoDto(salvo.getId(), salvo.getData(), salvo.getDescricao(), salvo.isAnual(), false);
    }

    @Transactional
    public void removerFeriado(Long id) {
        Feriado feriado = feriados.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feriado não encontrado."));
        feriados.delete(feriado);
    }
}
