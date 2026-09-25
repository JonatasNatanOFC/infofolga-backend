package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.CriarSolicitacaoDto;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.model.Role;
import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.model.TipoSolicitacao;
import com.infoway.infofolga.util.FotoUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.infoway.infofolga.repository.ColaboradorRepository;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    private Solicitacao buscar(Long idSolicitacao) {
        return solicitacaoRepository.findById(idSolicitacao)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));
    }

    private Colaborador buscarAvaliador(Long idAvaliador) {
        return colaboradorRepository.findById(idAvaliador)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliador não encontrado"));
    }

    private static final List<StatusSolicitation> STATUS_QUE_OCUPAM_DATA = List.of(
            StatusSolicitation.PENDENTE, StatusSolicitation.APROVADA,
            StatusSolicitation.ESTORNO_PENDENTE, StatusSolicitation.USUFRUIDA);

    // Mesmas regras de antecedência validadas no app (NovaSolicitacaoScreen).
    private static void validarAntecedencia(CriarSolicitacaoDto dto) {
        LocalDate hoje = LocalDate.now();
        boolean folga = dto.tipo() == TipoSolicitacao.FOLGA;
        int diasMinimos = folga ? 3 : 15;
        LocalDate limite = folga ? hoje.plusDays(30) : hoje.plusYears(2);

        if (dto.dataInicio().isBefore(hoje.plusDays(diasMinimos))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A solicitação deve ser feita com pelo menos " + diasMinimos + " dias de antecedência.");
        }
        if (dto.dataInicio().isAfter(limite)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data inicial está além do limite permitido para agendamento.");
        }
        if (folga && !dto.dataFim().equals(dto.dataInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Folga só pode ser solicitada para 1 único dia.");
        }
    }

    private static void exigirStatus(Solicitacao solicitacao, StatusSolicitation esperado) {
        if (solicitacao.getStatus() != esperado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Operação não permitida para solicitações com status " + solicitacao.getStatus() + ".");
        }
    }

    private static void exigirDono(Solicitacao solicitacao, Colaborador logado) {
        if (!solicitacao.getColaborador().getId().equals(logado.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso Negado.");
        }
    }

    @Transactional
    @CacheEvict(value = "dashboard_stats", allEntries = true)
    public Solicitacao criarSolicitacao(CriarSolicitacaoDto dto) {
        if (dto.dataFim().isBefore(dto.dataInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data final não pode ser anterior à data inicial.");
        }

        validarAntecedencia(dto);

        Colaborador logado = getColaboradorAutenticado();

        if (solicitacaoRepository.existeSobreposicao(logado.getId(), STATUS_QUE_OCUPAM_DATA,
                dto.dataInicio(), dto.dataFim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma solicitação sua para este período.");
        }

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

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = "dashboard_stats", allEntries = true)
    public Solicitacao aprovarSolicitacao(Long idSolicitacao, Long idAvaliador) {
        return avaliar(idSolicitacao, idAvaliador, StatusSolicitation.PENDENTE, StatusSolicitation.APROVADA);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = "dashboard_stats", allEntries = true)
    public Solicitacao rejeitarSolicitacao(Long idSolicitacao, Long idAvaliador, String motivo) {
        Solicitacao sol = avaliar(idSolicitacao, idAvaliador, StatusSolicitation.PENDENTE, StatusSolicitation.REJEITADA);
        sol.setMotivoResposta(motivo);
        return solicitacaoRepository.save(sol);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = "dashboard_stats", allEntries = true)
    public Solicitacao aprovarEstorno(Long idSolicitacao, Long idAvaliador) {
        return avaliar(idSolicitacao, idAvaliador, StatusSolicitation.ESTORNO_PENDENTE, StatusSolicitation.INVALIDADA);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = "dashboard_stats", allEntries = true)
    public Solicitacao rejeitarEstorno(Long idSolicitacao, Long idAvaliador) {
        return avaliar(idSolicitacao, idAvaliador, StatusSolicitation.ESTORNO_PENDENTE, StatusSolicitation.APROVADA);
    }

    private Solicitacao avaliar(Long idSolicitacao, Long idAvaliador,
                                StatusSolicitation statusEsperado, StatusSolicitation novoStatus) {
        Solicitacao sol = buscar(idSolicitacao);
        Colaborador avaliador = buscarAvaliador(idAvaliador);
        if (sol.getColaborador().getId().equals(avaliador.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não pode avaliar a sua própria solicitação.");
        }
        exigirStatus(sol, statusEsperado);

        sol.setStatus(novoStatus);
        sol.setAprovador(avaliador);
        return solicitacaoRepository.save(sol);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = "dashboard_stats", allEntries = true)
    public Solicitacao invalidarSolicitacao(Long idSolicitacao) {
        Colaborador logado = getColaboradorAutenticado();
        Solicitacao solicitacao = buscar(idSolicitacao);
        exigirDono(solicitacao, logado);
        exigirStatus(solicitacao, StatusSolicitation.APROVADA);

        solicitacao.setStatus(StatusSolicitation.ESTORNO_PENDENTE);
        return solicitacaoRepository.save(solicitacao);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = "dashboard_stats", allEntries = true)
    public void cancelarSolicitacao(Long idSolicitacao) {
        Colaborador logado = getColaboradorAutenticado();
        Solicitacao solicitacao = buscar(idSolicitacao);
        exigirDono(solicitacao, logado);
        exigirStatus(solicitacao, StatusSolicitation.PENDENTE);

        solicitacao.setStatus(StatusSolicitation.CANCELADA);
        solicitacaoRepository.save(solicitacao);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = "dashboard_stats", allEntries = true)
    public Solicitacao usufruirSolicitacao(Long idSolicitacao) {
        Colaborador logado = getColaboradorAutenticado();
        Solicitacao solicitacao = buscar(idSolicitacao);

        boolean isDono = solicitacao.getColaborador().getId().equals(logado.getId());
        boolean isGerencia = logado.getRole() == Role.GERENTE || logado.getRole() == Role.CEO;

        if (!isDono && !isGerencia) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso Negado. Você não tem permissão para alterar esta solicitação.");
        }
        exigirStatus(solicitacao, StatusSolicitation.APROVADA);

        solicitacao.setStatus(StatusSolicitation.USUFRUIDA);
        return solicitacaoRepository.save(solicitacao);
    }

    @Transactional(readOnly = true)
    public List<Solicitacao> listarMinhasSolicitacoes() {
        Colaborador logado = getColaboradorAutenticado();
        return solicitacaoRepository.findByColaboradorIdOtimizado(logado.getId());
    }

    @Transactional(readOnly = true)
    public List<Solicitacao> listarTodasParaGerencia(List<StatusSolicitation> status, List<TipoSolicitacao> tipos) {
        if (vazio(status) && vazio(tipos)) {
            return solicitacaoRepository.findAllOtimizado();
        }
        return solicitacaoRepository.listarPorFiltro(ouTodos(status, StatusSolicitation.values()),
                ouTodos(tipos, TipoSolicitacao.values()));
    }

    @Transactional(readOnly = true)
    public Page<Solicitacao> paginarParaGerencia(List<StatusSolicitation> status, List<TipoSolicitacao> tipos,
                                                 int pagina, int tamanho) {
        int tamanhoSeguro = Math.min(Math.max(tamanho, 1), 100);
        return solicitacaoRepository.buscarPorFiltro(ouTodos(status, StatusSolicitation.values()),
                ouTodos(tipos, TipoSolicitacao.values()), PageRequest.of(Math.max(pagina, 0), tamanhoSeguro));
    }

    @Transactional(readOnly = true)
    public Optional<FotoUtils.Imagem> buscarFotoHistorico(Long id, Colaborador logado) {
        Solicitacao solicitacao = buscar(id);
        boolean gestor = logado.getRole() == Role.CEO || logado.getRole() == Role.GERENTE;
        if (!gestor) {
            exigirDono(solicitacao, logado);
        }
        return FotoUtils.decodificar(solicitacao.getFotoHistorico());
    }

    private static boolean vazio(List<?> lista) {
        return lista == null || lista.isEmpty();
    }

    private static <T> List<T> ouTodos(List<T> lista, T[] todos) {
        return vazio(lista) ? List.of(todos) : lista;
    }
}
