package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.CadastroFuncionarioDto;
import com.infoway.infofolga.dto.UsuarioDto;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Gerente;
import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.repository.FuncionarioRepository;
import com.infoway.infofolga.repository.GerenteRepository;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import com.infoway.infofolga.util.CpfUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GerenciaFuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final GerenteRepository gerenteRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final PasswordEncoder passwordEncoder;

    public GerenciaFuncionarioService(FuncionarioRepository funcionarioRepository,
            GerenteRepository gerenteRepository,
            SolicitacaoRepository solicitacaoRepository,
            PasswordEncoder passwordEncoder) {
        this.funcionarioRepository = funcionarioRepository;
        this.gerenteRepository = gerenteRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioDto> listarFuncionarios() {
        return funcionarioRepository.findAll().stream().map(UsuarioDto::new).toList();
    }

    public UsuarioDto buscarPorId(Long id) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado."));
        return new UsuarioDto(funcionario);
    }

    public UsuarioDto buscarPorCpf(String cpf) {
        String cpfLimpo = CpfUtils.limpar(cpf);
        Funcionario funcionario = funcionarioRepository.findByCpf(cpfLimpo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado."));
        return new UsuarioDto(funcionario);
    }

    public boolean existeCpf(String cpf) {
        return funcionarioRepository.findByCpf(CpfUtils.limpar(cpf)).isPresent();
    }

    public UsuarioDto adicionarFuncionario(CadastroFuncionarioDto dto) {
        try {
            Funcionario funcionario = new Funcionario();
            atualizarDadosFuncionario(funcionario, dto);
            funcionario.setStatus("ativo");
            return new UsuarioDto(funcionarioRepository.save(funcionario));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF ou matrícula já cadastrado.");
        }
    }

    public UsuarioDto atualizarFuncionario(Long id, CadastroFuncionarioDto dto) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado."));
        try {
            atualizarDadosFuncionario(funcionario, dto);
            if (dto.status() != null && !dto.status().isBlank()) {
                funcionario.setStatus(dto.status());
            }
            return new UsuarioDto(funcionarioRepository.save(funcionario));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF ou matrícula já cadastrado.");
        }
    }

    @Transactional
    public void promoverParaGerente(Long funcionarioId) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado."));

        Gerente novoGerente = new Gerente();
        novoGerente.setNome(funcionario.getNome());
        novoGerente.setCpf(funcionario.getCpf());
        novoGerente.setSenha(funcionario.getSenha());
        novoGerente.setCargo("Gerente");
        novoGerente.setSetor(funcionario.getSetor());
        novoGerente.setMatricula(funcionario.getMatricula());
        novoGerente.setFoto(funcionario.getFoto());
        novoGerente.setStatus("ativo");
        novoGerente.setCeo(false);

        Gerente gerenteSalvo = gerenteRepository.saveAndFlush(novoGerente);

        List<Solicitacao> historico = solicitacaoRepository.findByFuncionarioId(funcionarioId);

        for (Solicitacao sol : historico) {
            sol.setFuncionario(null);
            sol.setSolicitanteGerente(gerenteSalvo);
        }

        solicitacaoRepository.saveAllAndFlush(historico);

        funcionarioRepository.delete(funcionario);
        funcionarioRepository.flush();
    }

    public void removerFuncionario(Long id) {
        if (!funcionarioRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado.");
        }
        funcionarioRepository.deleteById(id);
    }

    private void atualizarDadosFuncionario(Funcionario funcionario, CadastroFuncionarioDto dto) {
        funcionario.setNome(dto.nome());
        funcionario.setMatricula(nullIfBlank(dto.matricula()));
        funcionario.setCargo(nullIfBlank(dto.cargo()));
        funcionario.setSetor(nullIfBlank(dto.setor()));
        String cpfValido = nullIfBlank(dto.cpf());
        funcionario.setCpf(cpfValido != null ? CpfUtils.limpar(cpfValido) : null);
        funcionario.setFoto(nullIfBlank(dto.foto()));
        if (dto.senha() != null && !dto.senha().isBlank()) {
            funcionario.setSenha(passwordEncoder.encode(dto.senha()));
        }
    }

    private String nullIfBlank(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor;
    }
}