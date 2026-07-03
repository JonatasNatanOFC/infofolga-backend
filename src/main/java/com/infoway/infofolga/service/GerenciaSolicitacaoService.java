package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.SolicitacaoDto;
import com.infoway.infofolga.model.Gerente;
import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.repository.FuncionarioRepository;
import com.infoway.infofolga.repository.GerenteRepository; // <-- IMPORT ADICIONADO AQUI
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
    private final GerenteRepository gerenteRepository; // <-- VARIÁVEL ADICIONADA AQUI

    // <-- CONSTRUTOR ATUALIZADO PARA RECEBER O REPOSITÓRIO DO GERENTE
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
        return solicitacaoRepository.findByFuncionarioIdOrderByCriadoEmDesc(funcionarioId).stream().map(SolicitacaoDto::new).toList();
    }

    public SolicitacaoDto aprovarSolicitacao(Long id) {
        Solicitacao solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));

        validarAutoAprovacao(solicitacao);

        // 1. Pega o CPF de quem fez a requisição através do Token JWT
        String cpfLogado = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Busca o perfil de Gerente dessa pessoa no banco de dados
        Gerente gerenteAprovador = gerenteRepository.findByCpf(cpfLogado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Aprovador não encontrado ou sem permissão."));

        solicitacao.setStatus(StatusSolicitation.APROVADA);
        solicitacao.setMotivoResposta(null);

        // 3. Salva quem aprovou a folga!
        solicitacao.setGerente(gerenteAprovador);

        return new SolicitacaoDto(solicitacaoRepository.save(solicitacao));
    }

    public SolicitacaoDto rejeitarSolicitacao(Long id, String motivo) {
        Solicitacao solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));

        validarAutoAprovacao(solicitacao);

        // Adicionada a mesma lógica para registrar quem rejeitou!
        String cpfLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        Gerente gerenteAprovador = gerenteRepository.findByCpf(cpfLogado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Aprovador não encontrado ou sem permissão."));

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

    private void validarAutoAprovacao(Solicitacao solicitacao) {
        String cpfLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        if (solicitacao.getGerente() != null && solicitacao.getGerente().getCpf().equals(cpfLogado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pode aprovar ou rejeitar a sua própria solicitação.");
        }
    }
}