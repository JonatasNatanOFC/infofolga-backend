package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.AjusteSaldoDto;
import com.infoway.infofolga.dto.ColaboradorResumoDto;
import com.infoway.infofolga.dto.DiaExtratoDto;
import com.infoway.infofolga.dto.ExtratoDto;
import com.infoway.infofolga.dto.SaldoColaboradorDto;
import com.infoway.infofolga.model.AjusteSaldo;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.repository.AjusteSaldoRepository;
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
    private final AjusteSaldoRepository ajustes;

    public ExtratoService(ApuracaoDiaRepository apuracoes, ColaboradorRepository colaboradores,
                          RegrasPontuacaoService regras, AjusteSaldoRepository ajustes) {
        this.apuracoes = apuracoes;
        this.colaboradores = colaboradores;
        this.regras = regras;
        this.ajustes = ajustes;
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
        List<AjusteSaldo> ajustesDoMes = ajustes.findByColaboradorIdAndDataBetweenOrderByDataDesc(
                colaborador.getId(), mes.atDay(1), mes.atEndOfMonth());
        Map<Long, String> autores = colaboradores
                .findAllById(ajustesDoMes.stream().map(AjusteSaldo::getLancadoPor).distinct().toList()).stream()
                .collect(Collectors.toMap(Colaborador::getId, Colaborador::getNome));

        BigDecimal doMes = dias.stream().map(DiaExtratoDto::pontos).reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(ajustesDoMes.stream().map(AjusteSaldo::getPontos).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal saldo = apuracoes.saldoDe(colaborador.getId()).add(ajustes.somaDe(colaborador.getId()));
        return new ExtratoDto(new ColaboradorResumoDto(colaborador), saldo, doMes, mes.toString(), dias,
                ajustesDoMes.stream().map(a -> new AjusteSaldoDto(a, autores.get(a.getLancadoPor()))).toList());
    }

    /** CEO ou RH lança pontos à mão: saldo trazido de outro sistema ou correção. Ninguém ajusta o próprio saldo. */
    @Transactional
    public AjusteSaldoDto lancarAjuste(Long colaboradorId, AjusteSaldoDto dto, Colaborador autor) {
        Colaborador colaborador = comBancoDeHoras(colaboradorId);
        naoEhOProprio(autor, colaborador.getId());
        if (dto.pontos().signum() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ajuste precisa ter pontos diferentes de zero.");
        }
        AjusteSaldo ajuste = ajustes.save(new AjusteSaldo(colaborador.getId(), dto.data(), dto.pontos(),
                dto.justificativa().trim(), autor.getId()));
        return new AjusteSaldoDto(ajuste, autor.getNome());
    }

    @Transactional
    public void removerAjuste(Long ajusteId, Colaborador autor) {
        AjusteSaldo ajuste = ajustes.findById(ajusteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ajuste não encontrado."));
        naoEhOProprio(autor, ajuste.getColaboradorId());
        ajustes.delete(ajuste);
    }

    private Colaborador comBancoDeHoras(Long colaboradorId) {
        return colaboradores.findById(colaboradorId)
                .filter(regras::temBancoDeHoras)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado."));
    }

    private static void naoEhOProprio(Colaborador autor, Long colaboradorId) {
        if (autor.getId().equals(colaboradorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ninguém ajusta o próprio saldo: peça a outra pessoa do RH ou ao CEO.");
        }
    }

    /** Extrato de outro colaborador da mesma empresa (o @TenantId impede achar o de outra). */
    @Transactional(readOnly = true)
    public ExtratoDto extratoDe(Long colaboradorId, YearMonth mes) {
        return extrato(comBancoDeHoras(colaboradorId), mes);
    }

    /** Funcionários ativos com o saldo atual, em ordem alfabética. */
    @Transactional(readOnly = true)
    public List<SaldoColaboradorDto> equipe() {
        Map<Long, ApuracaoDiaRepository.SaldoResumo> saldos = apuracoes.saldosPorColaborador().stream()
                .collect(Collectors.toMap(ApuracaoDiaRepository.SaldoResumo::getColaboradorId, Function.identity()));
        Map<Long, BigDecimal> somasAjustes = ajustes.somasPorColaborador().stream()
                .collect(Collectors.toMap(AjusteSaldoRepository.SomaPorColaborador::getColaboradorId,
                        AjusteSaldoRepository.SomaPorColaborador::getSoma));
        boolean rhParticipa = regras.rhParticipaDoBancoDeHoras();
        return colaboradores.findAll().stream()
                .filter(c -> c.isEnabled() && c.temBancoDeHoras(rhParticipa))
                .sorted(Comparator.comparing(Colaborador::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(c -> {
                    var resumo = saldos.get(c.getId());
                    return new SaldoColaboradorDto(new ColaboradorResumoDto(c),
                            (resumo == null ? BigDecimal.ZERO : resumo.getSaldo())
                                    .add(somasAjustes.getOrDefault(c.getId(), BigDecimal.ZERO)),
                            resumo == null ? 0 : resumo.getDiasPendentes());
                })
                .toList();
    }
}
