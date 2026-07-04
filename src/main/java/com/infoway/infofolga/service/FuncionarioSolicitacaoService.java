package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.CriarSolicitacaoDto;
import com.infoway.infofolga.dto.SolicitacaoDto;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.model.TipoSolicitacao;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FuncionarioSolicitacaoService {

    private final SolicitacaoRepository solicitacaoRepository;

    public FuncionarioSolicitacaoService(SolicitacaoRepository solicitacaoRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
    }

    @Transactional
    public SolicitacaoDto criarSolicitacao(CriarSolicitacaoDto dto, Funcionario funcionario) {
        validarDatas(dto);

        if (dto.tipo() == TipoSolicitacao.FERIAS) {
            validarRegrasDeFerias(dto, funcionario.getId());
        }

        Solicitacao solicitacao = new Solicitacao();
        solicitacao.setFuncionario(funcionario);

        solicitacao.setNomeHistorico(funcionario.getNome());
        solicitacao.setCargoHistorico(funcionario.getCargo());
        solicitacao.setSetorHistorico(funcionario.getSetor());
        solicitacao.setFotoHistorico(funcionario.getFoto());

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

    @Transactional
    public void cancelarSolicitacao(Long solicitacaoId, Long userId) {
        Solicitacao solicitacao = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));

        if (solicitacao.getFuncionario() == null || !solicitacao.getFuncionario().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pode cancelar esta solicitação.");
        }

        if (solicitacao.getStatus() != StatusSolicitation.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Só é possível cancelar solicitações pendentes.");
        }

        solicitacaoRepository.delete(solicitacao);
    }

    @Transactional
    public SolicitacaoDto invalidarSolicitacao(Long id, Long userId) {
        Solicitacao solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));

        if (solicitacao.getFuncionario() == null || !solicitacao.getFuncionario().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta solicitação não pertence a você.");
        }

        if (solicitacao.getStatus() != StatusSolicitation.APROVADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Apenas solicitações APROVADAS podem solicitar estorno.");
        }

        // Bloqueio de data comentado para permitir testes dinâmicos em
        // homologação/desenvolvimento
        // if (LocalDate.now().isBefore(solicitacao.getDataInicio())) {
        // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você só pode
        // solicitar estorno pós-data.");
        // }

        solicitacao.setStatus(StatusSolicitation.ESTORNO_PENDENTE);
        solicitacao
                .setMotivoResposta("O funcionário declarou que não utilizou a folga. Aguardando validação do gerente.");

        return new SolicitacaoDto(solicitacaoRepository.save(solicitacao));
    }

    @Transactional
    public SolicitacaoDto usufruirSolicitacao(Long id, Long userId) {
        Solicitacao solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));

        if (solicitacao.getFuncionario() == null || !solicitacao.getFuncionario().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta solicitação não pertence a você.");
        }

        if (solicitacao.getStatus() != StatusSolicitation.APROVADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Apenas solicitações APROVADAS podem ser dadas como usufruídas.");
        }

        // Bloqueio de data comentado para testes rápidos
        // if (LocalDate.now().isBefore(solicitacao.getDataInicio())) {
        // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você só pode
        // confirmar após a data ter chegado.");
        // }

        solicitacao.setStatus(StatusSolicitation.USUFRUIDA);

        return new SolicitacaoDto(solicitacaoRepository.save(solicitacao));
    }

    private void validarDatas(CriarSolicitacaoDto dto) {
        if (dto.dataInicio().isAfter(dto.dataFim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data inicial não pode ser maior que a data final.");
        }

        LocalDate hoje = LocalDate.now();

        if (dto.tipo() == TipoSolicitacao.FOLGA) {
            LocalDate dataMinima = hoje.plusDays(3);
            LocalDate dataMaxima = hoje.plusDays(30);

            if (dto.dataInicio().isBefore(dataMinima)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A folga deve ser solicitada com pelo menos 3 dias de antecedência.");
            }
            if (dto.dataInicio().isAfter(dataMaxima)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Você só pode agendar folgas para no máximo 30 dias no futuro.");
            }
            if (!dto.dataInicio().isEqual(dto.dataFim())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A solicitação de folga deve ser para apenas 1 dia.");
            }

        } else if (dto.tipo() == TipoSolicitacao.FERIAS) {
            LocalDate dataMinimaFerias = hoje.plusDays(15);

            if (dto.dataInicio().isBefore(dataMinimaFerias)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Férias devem ser solicitadas com pelo menos 15 dias de antecedência.");
            }
        }
    }

    private void validarRegrasDeFerias(CriarSolicitacaoDto dto, Long userId) {
        int anoAtual = dto.dataInicio().getYear();
        long diasSolicitados = ChronoUnit.DAYS.between(dto.dataInicio(), dto.dataFim()) + 1;

        List<Solicitacao> historico = solicitacaoRepository.findByFuncionarioIdOrderByCriadoEmDesc(userId);

        List<Solicitacao> feriasDesteAno = historico.stream()
                .filter(s -> s.getTipo() == TipoSolicitacao.FERIAS)
                .filter(s -> s.getStatus() != StatusSolicitation.REJEITADA)
                .filter(s -> s.getDataInicio().getYear() == anoAtual)
                .toList();

        if (feriasDesteAno.size() >= 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Você já atingiu o limite máximo de 3 parcelamentos de férias neste ano.");
        }

        long diasJaTirados = feriasDesteAno.stream()
                .mapToLong(s -> ChronoUnit.DAYS.between(s.getDataInicio(), s.getDataFim()) + 1)
                .sum();

        if (diasJaTirados + diasSolicitados > 30) {
            long diasRestantes = 30 - diasJaTirados;
            if (diasRestantes == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Você já utilizou todos os seus 30 dias de férias neste ano.");
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O pedido (" + diasSolicitados + " dias) excede o limite. Você tem direito a apenas "
                                + diasRestantes + " dia(s) restante(s).");
            }
        }
    }
}