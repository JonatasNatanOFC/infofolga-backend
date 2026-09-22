package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.AtualizarPerfilDto;
import com.infoway.infofolga.dto.CadastroColaboradorDto;
import com.infoway.infofolga.dto.ColaboradorStatsDto;
import com.infoway.infofolga.dto.UsuarioDto;
import com.infoway.infofolga.dto.UsuarioResumoDto;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.model.Role;
import com.infoway.infofolga.repository.ColaboradorRepository;
import com.infoway.infofolga.util.CpfUtils;
import com.infoway.infofolga.util.FotoUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class ColaboradorService {

    private static final int TAMANHO_MINIMO_SENHA = 8;

    private final ColaboradorRepository colaboradorRepository;
    private final PasswordEncoder passwordEncoder;

    ColaboradorService(ColaboradorRepository colaboradorRepository, PasswordEncoder passwordEncoder) {
        this.colaboradorRepository = colaboradorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Cacheable(value = "stats_colaborador", key = "#idColaborador")
    public ColaboradorStatsDto getStats(Long idColaborador) {
        return new ColaboradorStatsDto(0, 0, 0, 0);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResumoDto> listarTodos() {
        return colaboradorRepository.findAll()
                .stream()
                .map(UsuarioResumoDto::new)
                .toList();
    }

    @Transactional
    public UsuarioDto atualizarPerfil(Long id, AtualizarPerfilDto dto) {
        Colaborador colaborador = buscar(id);

        if (dto.email() != null && !dto.email().isBlank()) {
            String email = dto.email().trim();
            if (!email.equalsIgnoreCase(colaborador.getEmail()) && colaboradorRepository.existsByEmailIgnoreCase(email)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um colaborador com este e-mail.");
            }
            colaborador.setEmail(email);
        }

        String novaFoto = FotoUtils.validarNovaFoto(dto.foto());
        if (novaFoto != null) {
            colaborador.setFoto(novaFoto);
        }

        if (dto.novaSenha() != null && !dto.novaSenha().isBlank()) {
            if (dto.senhaAtual() == null || !passwordEncoder.matches(dto.senhaAtual(), colaborador.getSenha())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha atual está incorreta.");
            }
            colaborador.setSenha(passwordEncoder.encode(dto.novaSenha()));
        }

        return new UsuarioDto(colaboradorRepository.save(colaborador));
    }

    public Optional<FotoUtils.Imagem> buscarFoto(Long id) {
        return FotoUtils.decodificar(buscar(id).getFoto());
    }

    public UsuarioDto buscarPorId(Long id) {
        return new UsuarioDto(buscar(id));
    }

    @Transactional
    @CacheEvict(value = "dashboard_stats", allEntries = true)
    public UsuarioDto cadastrar(CadastroColaboradorDto dto) {
        String cpf = validarCpf(dto.cpf());
        if (colaboradorRepository.existsByCpf(cpf)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um colaborador com este CPF.");
        }
        if (colaboradorRepository.existsByEmailIgnoreCase(dto.email().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um colaborador com este e-mail.");
        }

        Colaborador novo = new Colaborador();
        novo.setNome(dto.nome().trim());
        novo.setCpf(cpf);
        novo.setRole(dto.role() != null ? dto.role() : Role.FUNCIONARIO);
        novo.setSenha(passwordEncoder.encode(dto.senha()));
        novo.setEmail(dto.email().trim());
        novo.setCargo(dto.cargo());
        novo.setSetor(dto.setor());
        novo.setStatus("ativo");
        novo.setFoto(FotoUtils.validarNovaFoto(dto.foto()));

        return new UsuarioDto(colaboradorRepository.save(novo));
    }

    @Transactional
    public UsuarioDto atualizar(Long id, CadastroColaboradorDto dto) {
        Colaborador colaborador = buscar(id);

        if (dto.nome() != null && !dto.nome().isBlank()) colaborador.setNome(dto.nome().trim());

        if (dto.cpf() != null && !dto.cpf().isBlank() && !CpfUtils.limpar(dto.cpf()).equals(colaborador.getCpf())) {
            String cpf = validarCpf(dto.cpf());
            if (colaboradorRepository.existsByCpf(cpf)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um colaborador com este CPF.");
            }
            colaborador.setCpf(cpf);
        }

        if (dto.email() != null && !dto.email().isBlank()) {
            String email = dto.email().trim();
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de e-mail inválido");
            }
            if (!email.equalsIgnoreCase(colaborador.getEmail()) && colaboradorRepository.existsByEmailIgnoreCase(email)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um colaborador com este e-mail.");
            }
            colaborador.setEmail(email);
        }

        if (dto.cargo() != null) colaborador.setCargo(dto.cargo());
        if (dto.setor() != null) colaborador.setSetor(dto.setor());

        if (dto.role() != null && dto.role() != colaborador.getRole()) {
            impedirAlteracaoDoProprioUsuario(colaborador, "Você não pode alterar o seu próprio nível de acesso.");
            colaborador.setRole(dto.role());
        }

        String novaFoto = FotoUtils.validarNovaFoto(dto.foto());
        if (novaFoto != null) {
            colaborador.setFoto(novaFoto);
        }

        if (dto.senha() != null && !dto.senha().isBlank()) {
            if (dto.senha().length() < TAMANHO_MINIMO_SENHA) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A senha deve ter no mínimo " + TAMANHO_MINIMO_SENHA + " caracteres");
            }
            colaborador.setSenha(passwordEncoder.encode(dto.senha()));
        }

        return new UsuarioDto(colaboradorRepository.save(colaborador));
    }

    @Transactional
    @CacheEvict(value = "dashboard_stats", allEntries = true)
    public void deletar(Long id) {
        Colaborador colaborador = buscar(id);
        impedirAlteracaoDoProprioUsuario(colaborador, "Você não pode excluir a sua própria conta.");

        try {
            colaboradorRepository.delete(colaborador);
            colaboradorRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Não é possível excluir um colaborador que possui histórico. Utilize 'Remover Acesso'."
            );
        }
    }

    @Transactional
    public void inativar(Long id) {
        Colaborador colaborador = buscar(id);
        impedirAlteracaoDoProprioUsuario(colaborador, "Você não pode inativar a sua própria conta.");
        colaborador.setStatus("inativo");
        colaboradorRepository.save(colaborador);
    }

    @Transactional
    public void reativar(Long id) {
        Colaborador colaborador = buscar(id);
        colaborador.setStatus("ativo");
        colaboradorRepository.save(colaborador);
    }

    @Transactional
    public void promoverParaGerente(Long id) {
        alterarRoleDeNaoCeo(id, Role.GERENTE);
    }

    @Transactional
    public void rebaixarParaFuncionario(Long id) {
        alterarRoleDeNaoCeo(id, Role.FUNCIONARIO);
    }

    private void alterarRoleDeNaoCeo(Long id, Role novaRole) {
        Colaborador colaborador = buscar(id);
        if (colaborador.getRole() == Role.CEO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O nível de acesso de um CEO não pode ser alterado por esta operação.");
        }
        colaborador.setRole(novaRole);
        colaboradorRepository.save(colaborador);
    }

    private Colaborador buscar(Long id) {
        return colaboradorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado"));
    }

    private String validarCpf(String cpf) {
        if (!CpfUtils.isValido(cpf)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF inválido");
        }
        return CpfUtils.limpar(cpf);
    }

    private void impedirAlteracaoDoProprioUsuario(Colaborador alvo, String mensagem) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Colaborador logado && logado.getId().equals(alvo.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
        }
    }
}
