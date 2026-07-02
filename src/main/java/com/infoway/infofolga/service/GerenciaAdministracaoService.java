package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.FuncionarioPayload;
import com.infoway.infofolga.dto.GerentePayload;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Gerente;
import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.repository.FuncionarioRepository;
import com.infoway.infofolga.repository.GerenteRepository;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import com.infoway.infofolga.util.CpfUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GerenciaAdministracaoService {

    @Autowired
    private GerenteRepository gerenteRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private SolicitacaoRepository solicitacaoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void criarGerente(GerentePayload payload) {
        String cpfLimpo = CpfUtils.limpar(payload.cpf());
        if (gerenteRepository.findByCpf(cpfLimpo).isPresent() || funcionarioRepository.findByCpf(cpfLimpo).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF já cadastrado no sistema.");
        }
        Gerente novoGerente = new Gerente();
        novoGerente.setNome(payload.nome());
        novoGerente.setCpf(cpfLimpo);
        novoGerente.setSenha(passwordEncoder.encode(payload.senha()));
        novoGerente.setStatus("ativo");
        gerenteRepository.save(novoGerente);
    }

    public void criarFuncionario(FuncionarioPayload payload) {
        String cpfLimpo = CpfUtils.limpar(payload.cpf());
        if (funcionarioRepository.findByCpf(cpfLimpo).isPresent() || gerenteRepository.findByCpf(cpfLimpo).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF já cadastrado no sistema.");
        }
        Funcionario novoFuncionario = new Funcionario();
        novoFuncionario.setNome(payload.nome());
        novoFuncionario.setCpf(cpfLimpo);
        novoFuncionario.setSenha(passwordEncoder.encode(payload.senha()));
        novoFuncionario.setMatricula(payload.matricula());
        novoFuncionario.setCargo(payload.cargo());
        novoFuncionario.setSetor(payload.setor());
        novoFuncionario.setStatus(payload.status() != null ? payload.status() : "ativo");
        novoFuncionario.setFoto(payload.foto());
        funcionarioRepository.save(novoFuncionario);
    }

    public void atualizarFuncionario(Long id, FuncionarioPayload payload) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado."));
        String cpfLimpo = CpfUtils.limpar(payload.cpf());

        if (!funcionario.getCpf().equals(cpfLimpo)) {
            if (funcionarioRepository.findByCpf(cpfLimpo).isPresent() || gerenteRepository.findByCpf(cpfLimpo).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este CPF já pertence a outro usuário.");
            }
        }
        funcionario.setNome(payload.nome());
        funcionario.setCpf(cpfLimpo);
        funcionario.setMatricula(payload.matricula());
        funcionario.setCargo(payload.cargo());
        funcionario.setSetor(payload.setor());
        funcionario.setStatus(payload.status() != null ? payload.status() : funcionario.getStatus());
        funcionario.setFoto(payload.foto() != null ? payload.foto() : funcionario.getFoto());

        if (payload.senha() != null && !payload.senha().trim().isEmpty()) {
            funcionario.setSenha(passwordEncoder.encode(payload.senha()));
        }
        funcionarioRepository.save(funcionario);
    }

    @Transactional
    public void promoverParaGerente(Long funcionarioId) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado."));
        if (gerenteRepository.findByCpf(funcionario.getCpf()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este CPF já possui um cadastro ativo como Gerente.");
        }

        Gerente novoGerente = new Gerente();
        novoGerente.setNome(funcionario.getNome());
        novoGerente.setCpf(funcionario.getCpf());
        novoGerente.setSenha(funcionario.getSenha());
        novoGerente.setStatus("ativo");
        novoGerente = gerenteRepository.save(novoGerente);

        List<Solicitacao> solicitacoes = solicitacaoRepository.findByFuncionarioId(funcionarioId);
        for (Solicitacao solicitacao : solicitacoes) {
            solicitacao.setGerente(novoGerente);
            solicitacao.setFuncionario(null);
            solicitacaoRepository.save(solicitacao);
        }
        funcionarioRepository.delete(funcionario);
    }

    @Transactional
    public void rebaixarParaFuncionario(Long gerenteId) {
        Gerente gerente = gerenteRepository.findById(gerenteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gerente não encontrado."));
        if (funcionarioRepository.findByCpf(gerente.getCpf()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este CPF já possui um cadastro ativo como Funcionário.");
        }

        Funcionario novoFuncionario = new Funcionario();
        novoFuncionario.setNome(gerente.getNome());
        novoFuncionario.setCpf(gerente.getCpf());
        novoFuncionario.setSenha(gerente.getSenha());
        novoFuncionario.setStatus(gerente.getStatus());
        novoFuncionario.setMatricula("A DEFINIR");
        novoFuncionario.setCargo("A DEFINIR");
        novoFuncionario.setSetor("A DEFINIR");
        novoFuncionario = funcionarioRepository.save(novoFuncionario);

        List<Solicitacao> solicitacoes = solicitacaoRepository.findByGerenteId(gerenteId);
        for (Solicitacao solicitacao : solicitacoes) {
            solicitacao.setFuncionario(novoFuncionario);
            solicitacao.setGerente(null);
            solicitacaoRepository.save(solicitacao);
        }
        gerenteRepository.delete(gerente);
    }

    public void inativarGerente(Long gerenteId) {
        Gerente gerente = gerenteRepository.findById(gerenteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gerente não encontrado."));
        gerente.setStatus("inativo");
        gerenteRepository.save(gerente);
    }

    @Transactional
    public void aprovarSolicitacao(Long solicitacaoId) {
        Solicitacao solicitacao = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));

        String cpfLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        if (solicitacao.getGerente() != null && solicitacao.getGerente().getCpf().equals(cpfLogado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pode aprovar a sua própria solicitação de folga.");
        }
        solicitacao.setStatus(StatusSolicitation.APROVADA);
        solicitacaoRepository.save(solicitacao);
    }

    @Transactional
    public void rejeitarSolicitacao(Long solicitacaoId, String motivoRejeicao) {
        Solicitacao solicitacao = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));

        String cpfLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        if (solicitacao.getGerente() != null && solicitacao.getGerente().getCpf().equals(cpfLogado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pode rejeitar a sua própria solicitação de folga.");
        }
        solicitacao.setStatus(StatusSolicitation.REJEITADA);
        solicitacao.setMotivoResposta(motivoRejeicao);
        solicitacaoRepository.save(solicitacao);
    }
}