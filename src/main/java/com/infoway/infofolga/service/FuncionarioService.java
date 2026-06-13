package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.FuncionarioStatsDto;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FuncionarioService {

    private final SolicitacaoRepository solicitacaoRepository;

    public FuncionarioService(SolicitacaoRepository solicitacaoRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
    }

    public FuncionarioStatsDto getStats(Long funcionarioId) {
        long pendentes = solicitacaoRepository.countByFuncionarioIdAndStatus(funcionarioId, StatusSolicitation.PENDENTE);
        long aprovadas = solicitacaoRepository.countByFuncionarioIdAndStatus(funcionarioId, StatusSolicitation.APROVADA);
        long rejeitadas = solicitacaoRepository.countByFuncionarioIdAndStatus(funcionarioId, StatusSolicitation.REJEITADA);

        List<Solicitacao> aprovadasLista = solicitacaoRepository.findApproved(funcionarioId);
        long diasUsados = aprovadasLista.stream()
                .filter(s -> s.getDataInicio() != null && s.getDataFim() != null)
                .mapToLong(s -> ChronoUnit.DAYS.between(s.getDataInicio(), s.getDataFim()) + 1)
                .sum();

        return new FuncionarioStatsDto(pendentes, aprovadas, rejeitadas, diasUsados);
    }

    public Funcionario getFuncionarioAutenticado(Object principal) {
        if (principal instanceof Funcionario funcionario) {
            return funcionario;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem acesso a esta função de funcionário.");
    }
}