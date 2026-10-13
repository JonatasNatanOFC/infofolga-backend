package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.ColaboradorResumoDto;
import com.infoway.infofolga.dto.DiaExtratoDto;
import com.infoway.infofolga.dto.ExtratoDto;
import com.infoway.infofolga.dto.SaldoColaboradorDto;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.repository.ApuracaoDiaRepository;
import com.infoway.infofolga.repository.ColaboradorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Saldo e extrato de pontos. Lê só o que a apuração diária gravou; não recalcula nada.
 * shortcut: o saldo soma todos os dias, sem vencer horas pela validade da configuração;
 * aplicar o vencimento quando a empresa definir um prazo para o banco de horas.
 */
@Service
public class ExtratoService {

    private final ApuracaoDiaRepository apuracoes;
    private final ColaboradorRepository colaboradores;
    private final RegrasPontuacaoService regras;

    public ExtratoService(ApuracaoDiaRepository apuracoes, ColaboradorRepository colaboradores,
                          RegrasPontuacaoService regras) {
        this.apuracoes = apuracoes;
        this.colaboradores = colaboradores;
        this.regras = regras;
    }

    /** Extrato de quem está logado; perfis sem banco de horas recebem 403. */
    @Transactional(readOnly = true)
    public ExtratoDto meuExtrato(Colaborador logado, YearMonth mes) {
        if (!regras.temBancoDeHoras(logado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu perfil não participa do banco de horas.");
        }
        return extrato(logado, mes);
    }

    private ExtratoDto extrato(Colaborador colaborador, YearMonth mes) {
        List<DiaExtratoDto> dias = apuracoes
                .findByColaboradorIdAndDataBetweenOrderByDataDesc(colaborador.getId(), mes.atDay(1), mes.atEndOfMonth())
                .stream().map(DiaExtratoDto::new).toList();
        BigDecimal doMes = dias.stream().map(DiaExtratoDto::pontos).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ExtratoDto(new ColaboradorResumoDto(colaborador), apuracoes.saldoDe(colaborador.getId()), doMes,
                mes.toString(), dias);
    }

    /** Extrato de outro colaborador da mesma empresa (o @TenantId impede achar o de outra). */
    @Transactional(readOnly = true)
    public ExtratoDto extratoDe(Long colaboradorId, YearMonth mes) {
        Colaborador colaborador = colaboradores.findById(colaboradorId)
                .filter(regras::temBancoDeHoras)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado."));
        return extrato(colaborador, mes);
    }

    /** Funcionários ativos com o saldo atual, em ordem alfabética. */
    @Transactional(readOnly = true)
    public List<SaldoColaboradorDto> equipe() {
        Map<Long, ApuracaoDiaRepository.SaldoResumo> saldos = apuracoes.saldosPorColaborador().stream()
                .collect(Collectors.toMap(ApuracaoDiaRepository.SaldoResumo::getColaboradorId, Function.identity()));
        boolean rhParticipa = regras.rhParticipaDoBancoDeHoras();
        return colaboradores.findAll().stream()
                .filter(c -> c.isEnabled() && c.temBancoDeHoras(rhParticipa))
                .sorted(Comparator.comparing(Colaborador::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(c -> {
                    var resumo = saldos.get(c.getId());
                    return new SaldoColaboradorDto(new ColaboradorResumoDto(c),
                            resumo == null ? BigDecimal.ZERO : resumo.getSaldo(),
                            resumo == null ? 0 : resumo.getDiasPendentes());
                })
                .toList();
    }
}
