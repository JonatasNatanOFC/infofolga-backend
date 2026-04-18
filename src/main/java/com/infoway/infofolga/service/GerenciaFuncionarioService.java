package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.CadastroFuncionarioDto;
import com.infoway.infofolga.dto.UsuarioDto;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Role;
import com.infoway.infofolga.repository.FuncionarioRepository;
import com.infoway.infofolga.util.CpfUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GerenciaFuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;

    public GerenciaFuncionarioService(FuncionarioRepository funcionarioRepository,
                                      PasswordEncoder passwordEncoder) {
        this.funcionarioRepository = funcionarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioDto> listarFuncionarios() {
        List<Funcionario> funcionarios = funcionarioRepository.findAllByRole(Role.ROLE_FUNCIONARIO);

        System.out.println("=== SERVICE listarFuncionarios ===");
        for (Funcionario f : funcionarios) {
            System.out.println(
                    f.getId() + " | " + f.getNome() + " | " + f.getCpf() + " | " + f.getRole()
            );
        }
        System.out.println("=================================");

        return funcionarios.stream()
                .map(UsuarioDto::new)
                .toList();
    }

    public UsuarioDto buscarPorId(Long id) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Funcionário não encontrado."
                ));

        return new UsuarioDto(funcionario);
    }

    public UsuarioDto buscarPorCpf(String cpf) {
        String cpfLimpo = CpfUtils.limpar(cpf);

        Funcionario funcionario = funcionarioRepository.findByCpf(cpfLimpo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Funcionário não encontrado."
                ));

        return new UsuarioDto(funcionario);
    }

    public boolean existeCpf(String cpf) {
        String cpfLimpo = CpfUtils.limpar(cpf);
        return funcionarioRepository.findByCpf(cpfLimpo).isPresent();
    }

    public UsuarioDto adicionarFuncionario(CadastroFuncionarioDto dto) {
        try {
            Funcionario funcionario = new Funcionario();
            atualizarDadosFuncionario(funcionario, dto);
            funcionario.setRole(Role.ROLE_FUNCIONARIO);
            funcionario.setStatus("ativo");

            Funcionario salvo = funcionarioRepository.save(funcionario);
            return new UsuarioDto(salvo);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "CPF ou matrícula já cadastrado."
            );
        }
    }

    public UsuarioDto atualizarFuncionario(Long id, CadastroFuncionarioDto dto) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Funcionário não encontrado."
                ));

        try {
            atualizarDadosFuncionario(funcionario, dto);

            if (dto.status() != null && !dto.status().isBlank()) {
                funcionario.setStatus(dto.status());
            }

            Funcionario salvo = funcionarioRepository.save(funcionario);
            return new UsuarioDto(salvo);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "CPF ou matrícula já cadastrado."
            );
        }
    }

    public void removerFuncionario(Long id) {
        if (!funcionarioRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Funcionário não encontrado."
            );
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