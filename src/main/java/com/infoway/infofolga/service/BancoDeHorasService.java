package com.infoway.infofolga.service;

import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.model.TipoSolicitacao;
import com.infoway.infofolga.pontuacao.PoliticaSemSaldo;
import com.infoway.infofolga.repository.AjusteSaldoRepository;
import com.infoway.infofolga.repository.ApuracaoDiaRepository;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/** Saldo do banco de horas e se ele cobre uma folga, pela regra de folga sem saldo da empresa. */
@Service
public class BancoDeHorasService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final BigDecimal MINUTOS_POR_HORA = BigDecimal.valueOf(60);
    // Folgas que já contam contra o saldo, mesmo antes do dia ser apurado.
    private static final List<StatusSolicitation> FOLGA_RESERVADA = List.of(StatusSolicitation.PENDENTE,
            StatusSolicitation.APROVADA, StatusSolicitation.USUFRUIDA, StatusSolicitation.ESTORNO_PENDENTE);

    /**
     * @param saldoDisponivel saldo atual menos as folgas do banco já pedidas cujos dias ainda não foram apurados
     * @param pontosDaFolga   jornada prevista dos dias da folga (feriado e dia sem jornada não contam)
     */
    public record AvaliacaoFolga(BigDecimal saldoDisponivel, BigDecimal pontosDaFolga, PoliticaSemSaldo politica) {
        public boolean suficiente() {
            return saldoDisponivel.compareTo(pontosDaFolga) >= 0;
        }

        public boolean bloqueia() {
            return !suficiente() && politica == PoliticaSemSaldo.BLOQUEAR;
        }
    }

    private final ApuracaoDiaRepository apuracoes;
    private final AjusteSaldoRepository ajustes;
    private final SolicitacaoRepository solicitacoes;
    private final RegrasPontuacaoService regras;

    public BancoDeHorasService(ApuracaoDiaRepository apuracoes, AjusteSaldoRepository ajustes,
                               SolicitacaoRepository solicitacoes, RegrasPontuacaoService regras) {
        this.apuracoes = apuracoes;
        this.ajustes = ajustes;
        this.solicitacoes = solicitacoes;
        this.regras = regras;
    }

    @Transactional(readOnly = true)
    public BigDecimal saldoAtual(Long colaboradorId) {
        return apuracoes.saldoDe(colaboradorId).add(ajustes.somaDe(colaboradorId));
    }

    /** Vazio quando não se aplica: quem não tem banco de horas, ou empresa sem regras de pontuação. */
    @Transactional(readOnly = true)
    public Optional<AvaliacaoFolga> avaliarFolga(Colaborador colaborador, LocalDate inicio, LocalDate fim) {
        if (!regras.temBancoDeHoras(colaborador)) {
            return Optional.empty();
        }
        return regras.politicaSemSaldo().map(politica -> {
            BigDecimal reservado = BigDecimal.ZERO;
            // Folgas recentes ou futuras: as antigas já foram apuradas (ou nunca serão).
            for (Solicitacao s : solicitacoes.noPeriodo(colaborador.getId(), FOLGA_RESERVADA,
                    LocalDate.now(FUSO).minusDays(60), LocalDate.of(9999, 12, 31))) {
                if (s.getTipo() == TipoSolicitacao.FOLGA && Boolean.TRUE.equals(s.getDescontaSaldo())) {
                    reservado = reservado.add(horasNaoApuradas(colaborador.getId(), s.getDataInicio(), s.getDataFim()));
                }
            }
            return new AvaliacaoFolga(saldoAtual(colaborador.getId()).subtract(reservado),
                    horasPrevistas(inicio, fim), politica);
        });
    }

    private BigDecimal horasNaoApuradas(Long colaboradorId, LocalDate inicio, LocalDate fim) {
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            if (apuracoes.findByColaboradorIdAndData(colaboradorId, dia).isEmpty()) {
                total = total.add(horasPrevistas(dia, dia));
            }
        }
        return total;
    }

    private BigDecimal horasPrevistas(LocalDate inicio, LocalDate fim) {
        long minutos = 0;
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            LocalDate d = dia;
            if (!regras.ehFeriado(d)) {
                minutos += regras.regrasEm(d).map(r -> r.jornadaDe(d).toMinutes()).orElse(0L);
            }
        }
        return BigDecimal.valueOf(minutos).divide(MINUTOS_POR_HORA, 2, RoundingMode.HALF_UP);
    }
}
