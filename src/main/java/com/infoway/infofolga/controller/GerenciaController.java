package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.CadastroFuncionarioDto;
import com.infoway.infofolga.dto.DashboardStatsDto;
import com.infoway.infofolga.dto.UsuarioDto;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Role;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.repository.FuncionarioRepository;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/gerencia")
public class GerenciaController {

    private final FuncionarioRepository funcionarioRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final PasswordEncoder passwordEncoder; // 1. Dependência adicionada

    // Injeção de dependências via construtor
    public GerenciaController(FuncionarioRepository funcionarioRepository,
                              SolicitacaoRepository solicitacaoRepository,
                              PasswordEncoder passwordEncoder) {
        this.funcionarioRepository = funcionarioRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        long totalFuncionarios = funcionarioRepository.findAllByRole(Role.ROLE_FUNCIONARIO).size();
        long solicitacoesPendentes = solicitacaoRepository.countByStatus(StatusSolicitation.PENDENTE);
        LocalDateTime trintaDiasAtras = LocalDateTime.now().minusDays(30);
        long aprovadas30Dias = solicitacaoRepository.countByStatusAndAtualizadoEmAfter(StatusSolicitation.APROVADA, trintaDiasAtras);
        long rejeitadas30Dias = solicitacaoRepository.countByStatusAndAtualizadoEmAfter(StatusSolicitation.REJEITADA, trintaDiasAtras);

        var stats = new DashboardStatsDto(solicitacoesPendentes, totalFuncionarios, aprovadas30Dias, rejeitadas30Dias);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/funcionarios")
    public ResponseEntity<List<UsuarioDto>> getFuncionarios() {
        List<UsuarioDto> funcionarios = funcionarioRepository
                .findAllByRole(Role.ROLE_FUNCIONARIO)
                .stream()
                .map(UsuarioDto::new)
                .toList();
        return ResponseEntity.ok(funcionarios);
    }

    @GetMapping("/funcionarios/buscar-cpf/{cpf}")
    public ResponseEntity<UsuarioDto> getFuncionarioByCpf(@PathVariable String cpf) {
        String cpfFormatado = formatarCpf(cpf); // 3. Lógica extraída para um método auxiliar mais limpo

        return funcionarioRepository.findByCpfExato(cpf, cpfFormatado)
                .map(f -> ResponseEntity.ok(new UsuarioDto(f)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/funcionarios/{id}")
    public ResponseEntity<UsuarioDto> getFuncionario(@PathVariable Long id) {
        // 2. Previne erro 500 devolvendo um 404 apropriado se não existir
        return funcionarioRepository.findById(id)
                .map(funcionario -> ResponseEntity.ok(new UsuarioDto(funcionario)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/funcionarios")
    public ResponseEntity<UsuarioDto> adicionarFuncionario(@RequestBody CadastroFuncionarioDto dto) {
        Funcionario funcionario = new Funcionario();
        atualizarDadosFuncionario(funcionario, dto); // Reaproveita o código de atribuição

        funcionario.setRole(Role.ROLE_FUNCIONARIO);
        funcionario.setStatus("ativo");

        Funcionario saved = funcionarioRepository.save(funcionario);

        // 4. Devolve HTTP 201 Created em vez de 200 OK
        return ResponseEntity.status(HttpStatus.CREATED).body(new UsuarioDto(saved));
    }

    @PutMapping("/funcionarios/{id}")
    public ResponseEntity<UsuarioDto> atualizarFuncionario(@PathVariable Long id, @RequestBody CadastroFuncionarioDto dto) {
        Optional<Funcionario> funcionarioOptional = funcionarioRepository.findById(id);

        if (funcionarioOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Funcionario funcionario = funcionarioOptional.get();
        atualizarDadosFuncionario(funcionario, dto);

        if (dto.status() != null) {
            funcionario.setStatus(dto.status());
        }

        Funcionario saved = funcionarioRepository.save(funcionario);
        return ResponseEntity.ok(new UsuarioDto(saved));
    }

    @DeleteMapping("/funcionarios/{id}")
    public ResponseEntity<Void> removerFuncionario(@PathVariable Long id) {
        if (!funcionarioRepository.existsById(id)) {
            return ResponseEntity.notFound().build(); // Previne erro ao tentar apagar ID que não existe
        }
        funcionarioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- Métodos Auxiliares Privados ---

    private void atualizarDadosFuncionario(Funcionario funcionario, CadastroFuncionarioDto dto) {
        funcionario.setNome(dto.nome());
        funcionario.setMatricula(dto.matricula());
        funcionario.setCargo(dto.cargo());
        funcionario.setSetor(dto.setor());
        funcionario.setCpf(dto.cpf());
        funcionario.setFoto(dto.foto());

        if (dto.senha() != null && !dto.senha().isEmpty()) {
            // Utiliza o bean injetado em vez de criar uma nova instância
            funcionario.setSenha(passwordEncoder.encode(dto.senha()));
        }
    }

    private String formatarCpf(String cpf) {
        if (cpf == null) return null;
        return cpf.replaceAll("^(\\d{3})(\\d)", "$1.$2")
                .replaceAll("^(\\d{3})\\.(\\d{3})(\\d)", "$1.$2.$3")
                .replaceAll("^(\\d{3})\\.(\\d{3})\\.(\\d{3})(\\d{2})$", "$1.$2.$3-$4");
    }
}