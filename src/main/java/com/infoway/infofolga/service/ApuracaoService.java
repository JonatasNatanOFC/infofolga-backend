package com.infoway.infofolga.service;

import com.infoway.infofolga.model.ApuracaoDia;
import com.infoway.infofolga.model.Batida;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.model.TipoSolicitacao;
import com.infoway.infofolga.pontuacao.AgrupadorDeBatidas;
import com.infoway.infofolga.pontuacao.ApuracaoDiaria;
import com.infoway.infofolga.pontuacao.DiaTrabalho;
import com.infoway.infofolga.pontuacao.RegrasPontuacao;
import com.infoway.infofolga.pontuacao.SituacaoDia;
import com.infoway.infofolga.repository.ApuracaoDiaRepository;
import com.infoway.infofolga.repository.BatidaRepository;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Transforma as batidas guardadas em pontos de cada dia (tabela apuracao_dia).
 * Recalcular o mesmo período é seguro: o resultado de cada dia é substituído, nunca somado.
 */
@Service
public class ApuracaoService {

    // Folga ainda em verificação de estorno conta como folga até o gerente decidir.
    private static final List<StatusSolicitation> FOLGA_VALENDO =
            List.of(StatusSolicitation.APROVADA, StatusSolicitation.USUFRUIDA, StatusSolicitation.ESTORNO_PENDENTE);

    private final BatidaRepository batidas;
    private final ApuracaoDiaRepository apuracoes;
    private final SolicitacaoRepository solicitacoes;
    private final RegrasPontuacaoService regras;

    public ApuracaoService(BatidaRepository batidas, ApuracaoDiaRepository apuracoes,
                           SolicitacaoRepository solicitacoes, RegrasPontuacaoService regras) {
        this.batidas = batidas;
        this.apuracoes = apuracoes;
        this.solicitacoes = solicitacoes;
        this.regras = regras;
    }

    /** Recalcula os dias do período. Devolve quantos dias ficaram com resultado. */
    @Transactional
    public int reprocessar(Colaborador colaborador, LocalDate inicio, LocalDate fim) {
        if (!regras.temBancoDeHoras(colaborador)) {
            return 0;
        }
        // Um dia a mais em cada ponta: o turno da madrugada pode começar antes ou terminar depois do período.
        List<LocalDateTime> horarios = batidas.findByColaboradorIdAndDataHoraBetweenOrderByDataHora(
                        colaborador.getId(), inicio.minusDays(1).atStartOfDay(), fim.plusDays(2).atStartOfDay())
                .stream().map(Batida::getDataHora).toList();
        Map<LocalDate, List<LocalDateTime>> porDia = AgrupadorDeBatidas.porDia(horarios);
        List<Solicitacao> ausencias = solicitacoes.noPeriodo(colaborador.getId(), FOLGA_VALENDO, inicio, fim);

        int dias = 0;
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            Optional<RegrasPontuacao> regrasDoDia = regras.regrasEm(dia);
            List<LocalDateTime> doDia = porDia.getOrDefault(dia, List.of());
            Optional<SituacaoDia> ausencia = ausenciaEm(ausencias, dia);
            Optional<ApuracaoDia> existente = apuracoes.findByColaboradorIdAndData(colaborador.getId(), dia);

            // shortcut: dia sem batida e sem folga não vira falta, porque o AFD não traz atestados nem escalas;
            // marcar falta quando a fonte (a API do Faceponto tem faltas) informar.
            if (regrasDoDia.isEmpty() || (doDia.isEmpty() && ausencia.isEmpty())) {
                existente.ifPresent(apuracoes::delete);
                continue;
            }
            SituacaoDia situacao = ausencia.orElse(regras.ehFeriado(dia) ? SituacaoDia.FERIADO : SituacaoDia.NORMAL);
            var resultado = new ApuracaoDiaria(regrasDoDia.get()).apurar(new DiaTrabalho(dia, doDia, situacao));
            existente.ifPresentOrElse(
                    a -> a.atualizarCom(resultado),
                    () -> apuracoes.save(new ApuracaoDia(colaborador.getId(), resultado)));
            dias++;
        }
        return dias;
    }

    private static Optional<SituacaoDia> ausenciaEm(List<Solicitacao> ausencias, LocalDate dia) {
        return ausencias.stream()
                .filter(s -> !dia.isBefore(s.getDataInicio()) && !dia.isAfter(s.getDataFim()))
                .findFirst()
                .map(s -> s.getTipo() == TipoSolicitacao.FERIAS ? SituacaoDia.FERIAS
                        : Boolean.TRUE.equals(s.getDescontaSaldo()) ? SituacaoDia.FOLGA_COM_DESCONTO
                        : SituacaoDia.FOLGA_SEM_DESCONTO);
    }
}
