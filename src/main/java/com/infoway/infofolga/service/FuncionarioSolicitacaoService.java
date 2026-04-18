package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.CriarSolicitacaoDto;
import com.infoway.infofolga.dto.SolicitacaoDto;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FuncionarioSolicitacaoService {

    private final SolicitacaoRepository solicitacaoRepository;

    public FuncionarioSolicitacaoService(SolicitacaoRepository solicitacaoRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
    }

    public SolicitacaoDto criarSolicitacao(CriarSolicitacaoDto dto, Funcionario funcionario) {
        validarDatas(dto);

        Solicitacao solicitacao = new Solicitacao();
        solicitacao.setFuncionario(funcionario);
        solicitacao.setTipo(dto.tipo());
        solicitacao.setDataInicio(dto.dataInicio());
        solicitacao.setDataFim(dto.dataFim());
        solicitacao.setMotivo(dto.motivo());
        solicitacao.setStatus(StatusSolicitation.PENDENTE);

        Solicitacao salva = solicitacaoRepository.save(solicitacao);
        return new SolicitacaoDto(salva);
    }

    public List<SolicitacaoDto> listarMinhasSolicitacoes(Long funcionarioId) {
        return solicitacaoRepository.findByFuncionarioIdOrderByCriadoEmDesc(funcionarioId)
                .stream()
                .map(SolicitacaoDto::new)
                .toList();
    }

    public void cancelarSolicitacao(Long solicitacaoId, Long funcionarioId) {
        Solicitacao solicitacao = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Solicitação não encontrada."
                ));

        if (!solicitacao.getFuncionario().getId().equals(funcionarioId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você não pode cancelar esta solicitação."
            );
        }

        if (solicitacao.getStatus() != StatusSolicitation.PENDENTE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Só é possível cancelar solicitações pendentes."
            );
        }

        solicitacaoRepository.delete(solicitacao);
    }

    private void validarDatas(CriarSolicitacaoDto dto) {
        if (dto.dataInicio().isAfter(dto.dataFim())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A data inicial não pode ser maior que a data final."
            );
        }
    }
}