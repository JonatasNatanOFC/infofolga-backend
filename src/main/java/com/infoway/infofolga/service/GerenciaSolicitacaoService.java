package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.SolicitacaoDto;
import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.repository.FuncionarioRepository;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GerenciaSolicitacaoService {

    private final SolicitacaoRepository solicitacaoRepository;
    private final FuncionarioRepository funcionarioRepository;

    public GerenciaSolicitacaoService(SolicitacaoRepository solicitacaoRepository,
                                      FuncionarioRepository funcionarioRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.funcionarioRepository = funcionarioRepository;
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
        return solicitacaoRepository.findByFuncionarioIdOrderByCriadoEmDesc(funcionarioId).stream().map(SolicitacaoDto::new).toList();
    }

    public SolicitacaoDto aprovarSolicitacao(Long id) {
        Solicitacao solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));

        validarAutoAprovacao(solicitacao); // <-- Verificação adicionada

        solicitacao.setStatus(StatusSolicitation.APROVADA);
        solicitacao.setMotivoResposta(null);

        return new SolicitacaoDto(solicitacaoRepository.save(solicitacao));
    }

    public SolicitacaoDto rejeitarSolicitacao(Long id, String motivo) {
        Solicitacao solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));

        validarAutoAprovacao(solicitacao); // <-- Verificação adicionada

        solicitacao.setStatus(StatusSolicitation.REJEITADA);
        solicitacao.setMotivoResposta(motivo);

        return new SolicitacaoDto(solicitacaoRepository.save(solicitacao));
    }

    public void removerSolicitacao(Long id) {
        if (!solicitacaoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada.");
        }
        solicitacaoRepository.deleteById(id);
    }

    // Lógica injetada para garantir a segurança da operação
    private void validarAutoAprovacao(Solicitacao solicitacao) {
        String cpfLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        if (solicitacao.getGerente() != null && solicitacao.getGerente().getCpf().equals(cpfLogado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pode aprovar ou rejeitar a sua própria solicitação.");
        }
    }
}