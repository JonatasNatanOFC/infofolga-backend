package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.CriarSolicitacaoDto;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.repository.ColaboradorRepository;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class SolicitacaoService {

    private final SolicitacaoRepository solicitacaoRepository;
    private final ColaboradorRepository colaboradorRepository;

    SolicitacaoService(SolicitacaoRepository solicitacaoRepository, ColaboradorRepository colaboradorRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.colaboradorRepository = colaboradorRepository;
    }

    private Colaborador getColaboradorAutenticado() {
        return (Colaborador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @CacheEvict(value = {"stats_colaborador", "dashboard_stats"}, allEntries = true)
    public Solicitacao criarSolicitacao(CriarSolicitacaoDto dto) {
        Colaborador logado = getColaboradorAutenticado();

        Solicitacao solicitacao = new Solicitacao();
        solicitacao.setColaborador(logado);
        solicitacao.setDataInicio(dto.dataInicio());
        solicitacao.setDataFim(dto.dataFim());
        solicitacao.setMotivo(dto.motivo());
        solicitacao.setTipo(dto.tipo());
        solicitacao.setStatus(StatusSolicitation.PENDENTE);

        solicitacao.setNomeHistorico(logado.getNome());
        solicitacao.setCargoHistorico(logado.getCargo());
        solicitacao.setSetorHistorico(logado.getSetor());
        solicitacao.setFotoHistorico(logado.getFoto());

        return solicitacaoRepository.save(solicitacao);
    }

    @CacheEvict(value = {"stats_colaborador", "dashboard_stats"}, allEntries = true)
    public Solicitacao aprovarSolicitacao(Long idSolicitacao, Long idAvaliador) {
        Solicitacao sol = solicitacaoRepository.findById(idSolicitacao)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));

        Colaborador avaliador = colaboradorRepository.findById(idAvaliador)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliador não encontrado"));

        sol.setStatus(StatusSolicitation.APROVADA);
        sol.setAprovador(avaliador);
        return solicitacaoRepository.save(sol);
    }

    @CacheEvict(value = {"stats_colaborador", "dashboard_stats"}, allEntries = true)
    public Solicitacao rejeitarSolicitacao(Long idSolicitacao, Long idAvaliador, String motivo) {
        Solicitacao sol = solicitacaoRepository.findById(idSolicitacao)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));

        Colaborador avaliador = colaboradorRepository.findById(idAvaliador)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliador não encontrado"));

        sol.setStatus(StatusSolicitation.REJEITADA);
        sol.setAprovador(avaliador);
        sol.setMotivoResposta(motivo);
        return solicitacaoRepository.save(sol);
    }

    @CacheEvict(value = {"stats_colaborador", "dashboard_stats"}, allEntries = true)
    public Solicitacao aprovarEstorno(Long idSolicitacao, Long idAvaliador) {
        Solicitacao sol = solicitacaoRepository.findById(idSolicitacao)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));

        Colaborador avaliador = colaboradorRepository.findById(idAvaliador)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliador não encontrado"));

        sol.setStatus(StatusSolicitation.INVALIDADA);
        sol.setAprovador(avaliador);
        return solicitacaoRepository.save(sol);
    }

    @CacheEvict(value = {"stats_colaborador", "dashboard_stats"}, allEntries = true)
    public Solicitacao rejeitarEstorno(Long idSolicitacao, Long idAvaliador) {
        Solicitacao sol = solicitacaoRepository.findById(idSolicitacao)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));

        Colaborador avaliador = colaboradorRepository.findById(idAvaliador)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliador não encontrado"));

        sol.setStatus(StatusSolicitation.APROVADA);
        sol.setAprovador(avaliador);
        return solicitacaoRepository.save(sol);
    }

    @CacheEvict(value = {"stats_colaborador", "dashboard_stats"}, allEntries = true)
    public Solicitacao invalidarSolicitacao(Long idSolicitacao) {
        Colaborador logado = getColaboradorAutenticado();
        Objects.requireNonNull(idSolicitacao, "O ID da solicitação não pode ser nulo");
        Solicitacao solicitacao = solicitacaoRepository.findById(idSolicitacao)
                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada"));

        if (!solicitacao.getColaborador().getId().equals(logado.getId())) {
            throw new RuntimeException("Acesso Negado.");
        }

        solicitacao.setStatus(StatusSolicitation.ESTORNO_PENDENTE);
        return solicitacaoRepository.save(solicitacao);
    }

    @CacheEvict(value = {"stats_colaborador", "dashboard_stats"}, allEntries = true)
    public void cancelarSolicitacao(Long idSolicitacao) {
        Colaborador logado = getColaboradorAutenticado();
        Solicitacao solicitacao = solicitacaoRepository.findById(idSolicitacao)
                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada"));

        if (!solicitacao.getColaborador().getId().equals(logado.getId())) {
            throw new RuntimeException("Acesso Negado.");
        }

        solicitacao.setStatus(StatusSolicitation.CANCELADA);
        solicitacaoRepository.save(solicitacao);
    }

    @CacheEvict(value = {"stats_colaborador", "dashboard_stats"}, allEntries = true)
    public Solicitacao usufruirSolicitacao(Long idSolicitacao) {
        Colaborador logado = getColaboradorAutenticado();
        Objects.requireNonNull(idSolicitacao, "O ID da solicitação não pode ser nulo");
        Solicitacao solicitacao = solicitacaoRepository.findById(idSolicitacao)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));

        boolean isDono = solicitacao.getColaborador().getId().equals(logado.getId());
        boolean isGerencia = logado.getRole().name().equals("GERENTE") || logado.getRole().name().equals("CEO");

        if (!isDono && !isGerencia) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso Negado. Você não tem permissão para alterar esta solicitação.");
        }

        solicitacao.setStatus(StatusSolicitation.USUFRUIDA);
        return solicitacaoRepository.save(solicitacao);
    }

    public List<Solicitacao> listarMinhasSolicitacoes() {
        Colaborador logado = getColaboradorAutenticado();
        return solicitacaoRepository.findByColaboradorIdOtimizado(logado.getId());
    }

    public List<Solicitacao> listarTodasParaGerencia() {
        return solicitacaoRepository.findAllOtimizado();
    }
}