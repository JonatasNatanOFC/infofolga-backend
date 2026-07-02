package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.GerentePayload;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Gerente;
import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.repository.FuncionarioRepository;
import com.infoway.infofolga.repository.GerenteRepository;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import com.infoway.infofolga.util.CpfUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GerenciaAdministracaoService {

    private final GerenteRepository gerenteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final PasswordEncoder passwordEncoder;

    public GerenciaAdministracaoService(GerenteRepository gerenteRepository,
                                        FuncionarioRepository funcionarioRepository,
                                        SolicitacaoRepository solicitacaoRepository,
                                        PasswordEncoder passwordEncoder) {
        this.gerenteRepository = gerenteRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
        novoGerente.setCeo(false);
        novoGerente.setMatricula(payload.matricula());
        novoGerente.setCargo(payload.cargo());
        novoGerente.setSetor(payload.setor());
        novoGerente.setFoto(payload.foto());

        gerenteRepository.save(novoGerente);
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
        novoGerente.setCeo(false);
        novoGerente.setMatricula(funcionario.getMatricula());
        novoGerente.setCargo(funcionario.getCargo());
        novoGerente.setSetor(funcionario.getSetor());
        novoGerente.setFoto(funcionario.getFoto());

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

        if (gerente.isCeo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O perfil de CEO não pode ser rebaixado.");
        }

        if (funcionarioRepository.findByCpf(gerente.getCpf()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este CPF já possui um cadastro ativo como Funcionário.");
        }

        Funcionario novoFuncionario = new Funcionario();
        novoFuncionario.setNome(gerente.getNome());
        novoFuncionario.setCpf(gerente.getCpf());
        novoFuncionario.setSenha(gerente.getSenha());
        novoFuncionario.setStatus(gerente.getStatus());
        novoFuncionario.setMatricula(gerente.getMatricula());
        novoFuncionario.setCargo(gerente.getCargo());
        novoFuncionario.setSetor(gerente.getSetor());
        novoFuncionario.setFoto(gerente.getFoto());

        novoFuncionario = funcionarioRepository.save(novoFuncionario);

        List<Solicitacao> solicitacoes = solicitacaoRepository.findByGerenteId(gerenteId);
        for (Solicitacao solicitacao : solicitacoes) {
            solicitacao.setFuncionario(novoFuncionario);
            solicitacao.setGerente(null);
            solicitacaoRepository.save(solicitacao);
        }

        gerenteRepository.delete(gerente);
    }

    @Transactional
    public void atualizarGerente(Long id, GerentePayload payload) {
        Gerente gerente = gerenteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gerente não encontrado."));

        String cpfLimpo = payload.cpf() != null ? CpfUtils.limpar(payload.cpf()) : gerente.getCpf();

        if (!gerente.getCpf().equals(cpfLimpo)) {
            if (gerenteRepository.findByCpf(cpfLimpo).isPresent() || funcionarioRepository.findByCpf(cpfLimpo).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este CPF já pertence a outro usuário.");
            }
        }

        gerente.setNome(payload.nome() != null ? payload.nome() : gerente.getNome());
        gerente.setCpf(cpfLimpo);
        gerente.setMatricula(payload.matricula() != null ? payload.matricula() : gerente.getMatricula());
        gerente.setCargo(payload.cargo() != null ? payload.cargo() : gerente.getCargo());
        gerente.setSetor(payload.setor() != null ? payload.setor() : gerente.getSetor());
        gerente.setFoto(payload.foto() != null ? payload.foto() : gerente.getFoto());

        if (payload.senha() != null && !payload.senha().trim().isEmpty()) {
            gerente.setSenha(passwordEncoder.encode(payload.senha()));
        }

        gerenteRepository.save(gerente);
    }

    public void inativarGerente(Long gerenteId) {
        Gerente gerente = gerenteRepository.findById(gerenteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gerente não encontrado."));

        if (gerente.isCeo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O perfil de CEO não pode ser inativado.");
        }

        gerente.setStatus("inativo");
        gerenteRepository.save(gerente);
    }
}