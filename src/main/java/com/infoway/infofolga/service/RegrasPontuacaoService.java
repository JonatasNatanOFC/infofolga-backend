package com.infoway.infofolga.service;

import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.model.ConfiguracaoPontuacao;
import com.infoway.infofolga.pontuacao.FeriadosNacionais;
import com.infoway.infofolga.pontuacao.PoliticaSemSaldo;
import com.infoway.infofolga.pontuacao.RegrasPontuacao;
import com.infoway.infofolga.repository.ConfiguracaoPontuacaoRepository;
import com.infoway.infofolga.repository.FeriadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

/** Regras e calendário da empresa do usuário atual (filtrados pelo @TenantId). */
@Service
public class RegrasPontuacaoService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

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

    /** Se o RH da empresa participa do banco de horas, pelas regras em vigor hoje. */
    @Transactional(readOnly = true)
    public boolean rhParticipaDoBancoDeHoras() {
        return configuracoes.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(LocalDate.now(FUSO))
                .map(ConfiguracaoPontuacao::isRhTemBancoDeHoras)
                .orElse(false);
    }

    /** O que fazer quando pedem folga sem saldo, pelas regras em vigor hoje; vazio se a empresa não tem regras. */
    @Transactional(readOnly = true)
    public Optional<PoliticaSemSaldo> politicaSemSaldo() {
        return configuracoes.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(LocalDate.now(FUSO))
                .map(ConfiguracaoPontuacao::getPoliticaSemSaldo);
    }

    public boolean temBancoDeHoras(Colaborador colaborador) {
        return colaborador.temBancoDeHoras(rhParticipaDoBancoDeHoras());
    }

    @Transactional(readOnly = true)
    public boolean ehFeriado(LocalDate data) {
        return FeriadosNacionais.doAno(data.getYear()).containsKey(data) || feriados.existeEm(data);
    }
}
