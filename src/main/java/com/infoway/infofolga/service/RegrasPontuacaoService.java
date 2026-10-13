package com.infoway.infofolga.service;

import com.infoway.infofolga.model.ConfiguracaoPontuacao;
import com.infoway.infofolga.pontuacao.FeriadosNacionais;
import com.infoway.infofolga.pontuacao.RegrasPontuacao;
import com.infoway.infofolga.repository.ConfiguracaoPontuacaoRepository;
import com.infoway.infofolga.repository.FeriadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/** Regras e calendário da empresa do usuário atual (filtrados pelo @TenantId). */
@Service
public class RegrasPontuacaoService {

    private final ConfiguracaoPontuacaoRepository configuracoes;
    private final FeriadoRepository feriados;

    public RegrasPontuacaoService(ConfiguracaoPontuacaoRepository configuracoes, FeriadoRepository feriados) {
        this.configuracoes = configuracoes;
        this.feriados = feriados;
    }

    /** Vazio quando a pontuação ainda não tinha começado na data: o dia não é apurado. */
    @Transactional(readOnly = true)
    public Optional<RegrasPontuacao> regrasEm(LocalDate data) {
        return configuracoes.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(data)
                .map(ConfiguracaoPontuacao::paraRegras);
    }

    @Transactional(readOnly = true)
    public boolean ehFeriado(LocalDate data) {
        return FeriadosNacionais.doAno(data.getYear()).containsKey(data) || feriados.existsByData(data);
    }
}
