package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.CadastroColaboradorDto;
import com.infoway.infofolga.dto.ColaboradorStatsDto;
import com.infoway.infofolga.dto.UsuarioDto;
import com.infoway.infofolga.dto.UsuarioResumoDto;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.model.Role; // 🟢 Importação obrigatória do seu Enum Role
import com.infoway.infofolga.repository.ColaboradorRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ColaboradorService {

    private final ColaboradorRepository colaboradorRepository;
    private final PasswordEncoder passwordEncoder;

    ColaboradorService(ColaboradorRepository colaboradorRepository, PasswordEncoder passwordEncoder) {
        this.colaboradorRepository = colaboradorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Cacheable(value = "stats_colaborador", key = "#idColaborador")
    public ColaboradorStatsDto getStats(Long idColaborador) {
        System.out.println("Gerando estatísticas para o colaborador ID: " + idColaborador);
        return new ColaboradorStatsDto(0, 0, 0, 0);
    }

    public List<UsuarioResumoDto> listarTodos() {
        return colaboradorRepository.findAll()
                .stream()
                .map(UsuarioResumoDto::new)
                .collect(Collectors.toList());
    }

    public UsuarioDto cadastrar(CadastroColaboradorDto dto) {
        Colaborador novo = new Colaborador();
        novo.setNome(dto.nome());
        novo.setCpf(dto.cpf());
        novo.setRole(dto.role() != null ? dto.role() : Role.FUNCIONARIO);
        novo.setSenha(passwordEncoder.encode(dto.senha()));
        novo.setEmail(dto.email());
        novo.setCargo(dto.cargo());
        novo.setSetor(dto.setor());
        novo.setStatus("ativo");

        novo.setFoto(dto.foto());

        Colaborador salvo = colaboradorRepository.save(novo);
        return new UsuarioDto(salvo);
    }

    public UsuarioDto atualizar(Long id, CadastroColaboradorDto dto) {
        Colaborador colaborador = colaboradorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado"));

        if (dto.nome() != null) colaborador.setNome(dto.nome());
        if (dto.cpf() != null) colaborador.setCpf(dto.cpf());
        if (dto.email() != null) colaborador.setEmail(dto.email());
        if (dto.cargo() != null) colaborador.setCargo(dto.cargo());
        if (dto.setor() != null) colaborador.setSetor(dto.setor());

        if (dto.role() != null) {
            colaborador.setRole(dto.role());
        }

        if (dto.foto() != null && !dto.foto().isEmpty()) {
            colaborador.setFoto(dto.foto());
        }

        if (dto.senha() != null && !dto.senha().isBlank()) {
            colaborador.setSenha(passwordEncoder.encode(dto.senha()));
        }

        Colaborador atualizado = colaboradorRepository.save(colaborador);
        return new UsuarioDto(atualizado);
    }

    public void deletar(Long id) {
        if (!colaboradorRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado");
        }

        try {
            colaboradorRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Não é possível excluir um colaborador que possui histórico. Utilize 'Remover Acesso'."
            );
        }
    }

    public void inativar(Long id) {
        Colaborador colaborador = colaboradorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado"));
        colaborador.setStatus("inativo");
        colaboradorRepository.save(colaborador);
    }

    public void reativar(Long id) {
        Colaborador colaborador = colaboradorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado"));
        colaborador.setStatus("ativo");
        colaboradorRepository.save(colaborador);
    }

    public void promoverParaGerente(Long id) {
        Colaborador colaborador = colaboradorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado"));
        colaborador.setRole(Role.GERENTE);
        colaboradorRepository.save(colaborador);
    }

    public void rebaixarParaFuncionario(Long id) {
        Colaborador colaborador = colaboradorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado"));
        colaborador.setRole(Role.FUNCIONARIO);
        colaboradorRepository.save(colaborador);
    }
}