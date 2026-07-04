package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.SolicitacaoDto;
import com.infoway.infofolga.model.Gerente;
import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.repository.FuncionarioRepository;
import com.infoway.infofolga.repository.GerenteRepository;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GerenciaSolicitacaoService {

    private final SolicitacaoRepository solicitacaoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final GerenteRepository gerenteRepository;

    public GerenciaSolicitacaoService(SolicitacaoRepository solicitacaoRepository,
            FuncionarioRepository funcionarioRepository,
            GerenteRepository gerenteRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.gerenteRepository = gerenteRepository;
    }

    public List<SolicitacaoDto> listarSolicitacoes() {
        return solicitacaoRepository.findAll().stream().map(SolicitacaoDto::new).toList();
    }

    public List<SolicitacaoDto> listarPorStatus(StatusSolicitation status) {
        return solicitacaoRepository.findByStatusOrderByCriadoEmDesc(status).stream().map(SolicitacaoDto::new).toList();
    }

    public List<SolicitacaoDto> listarPorFuncionario(Long funcionarioId) {
        if (!funcionarioRepository.existsById(funcionarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado.");
        }
        return solicitacaoRepository.findByFuncionarioIdOrderByCriadoEmDesc(funcionarioId).stream()
                .map(SolicitacaoDto::new).toList();
    }

    public SolicitacaoDto aprovarSolicitacao(Long id) {
        Solicitacao solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));

        Gerente gerenteAprovador = obterGerenteLogado();

        validarPermissaoAprovacao(solicitacao, gerenteAprovador);

        solicitacao.setStatus(StatusSolicitation.APROVADA);
        solicitacao.setMotivoResposta(null);
        solicitacao.setGerente(gerenteAprovador);

        return new SolicitacaoDto(solicitacaoRepository.save(solicitacao));
    }

    public SolicitacaoDto rejeitarSolicitacao(Long id, String motivo) {
        Solicitacao solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));

        Gerente gerenteAprovador = obterGerenteLogado();

        validarPermissaoAprovacao(solicitacao, gerenteAprovador);

        solicitacao.setStatus(StatusSolicitation.REJEITADA);
        solicitacao.setMotivoResposta(motivo);
        solicitacao.setGerente(gerenteAprovador);

        return new SolicitacaoDto(solicitacaoRepository.save(solicitacao));
    }

    public void removerSolicitacao(Long id) {
        if (!solicitacaoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada.");
        }
        solicitacaoRepository.deleteById(id);
    }

    @Transactional
    public SolicitacaoDto invalidarSolicitacao(Long id, String motivo) {
        Solicitacao solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));

        Gerente gerenteAprovador = obterGerenteLogado();
        validarPermissaoAprovacao(solicitacao, gerenteAprovador);

        if (solicitacao.getStatus() != StatusSolicitation.APROVADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Apenas solicitações APROVADAS podem ser Invalidadas.");
        }

        if (java.time.LocalDate.now().isBefore(solicitacao.getDataInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Você só pode invalidar uma folga no próprio dia ou após a data ter passado.");
        }

        solicitacao.setStatus(StatusSolicitation.INVALIDADA);
        solicitacao.setMotivoResposta("Folga Estornada/Invalidada. Motivo: " + motivo);
        solicitacao.setGerente(gerenteAprovador);

        return new SolicitacaoDto(solicitacaoRepository.save(solicitacao));
    }

    @Transactional
    public SolicitacaoDto usufruirSolicitacao(Long id) {
        Solicitacao solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));

        Gerente gerenteAprovador = obterGerenteLogado();
        validarPermissaoAprovacao(solicitacao, gerenteAprovador);

        if (solicitacao.getStatus() != StatusSolicitation.APROVADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Apenas solicitações APROVADAS podem ser confirmadas como Usufruídas.");
        }

        if (java.time.LocalDate.now().isBefore(solicitacao.getDataInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Você só pode confirmar o uso após a data de início ter chegado.");
        }

        solicitacao.setStatus(StatusSolicitation.USUFRUIDA);
        solicitacao.setGerente(gerenteAprovador);

        return new SolicitacaoDto(solicitacaoRepository.save(solicitacao));
    }

    private Gerente obterGerenteLogado() {
        String cpfLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        return gerenteRepository.findByCpf(cpfLogado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Aprovador não encontrado ou sem permissão."));
    }

    private void validarPermissaoAprovacao(Solicitacao solicitacao, Gerente gerenteAprovador) {
        String cargo = solicitacao.getFuncionario() != null ? solicitacao.getFuncionario().getCargo()
                : solicitacao.getCargoHistorico();
        boolean isFolgaDeGerente = cargo != null && cargo.toLowerCase().contains("gerente");

        if (isFolgaDeGerente && !gerenteAprovador.isCeo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas o CEO possui hierarquia para avaliar as solicitações do histórico de gerentes.");
        }

        String nomeDonoFolga = solicitacao.getFuncionario() != null ? solicitacao.getFuncionario().getNome()
                : solicitacao.getNomeHistorico();
        if (nomeDonoFolga != null && nomeDonoFolga.equalsIgnoreCase(gerenteAprovador.getNome())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não pode aprovar ou rejeitar uma solicitação sua (mesmo as do passado).");
        }
    }
}