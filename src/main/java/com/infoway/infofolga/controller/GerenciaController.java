package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.CadastroFuncionarioDto;
import com.infoway.infofolga.dto.DashboardStatsDto;
import com.infoway.infofolga.dto.SolicitacaoDto;
import com.infoway.infofolga.dto.UsuarioDto;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Role;
import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.repository.FuncionarioRepository;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import com.infoway.infofolga.util.CpfUtils;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final PasswordEncoder passwordEncoder;

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
        long aprovadas30Dias = solicitacaoRepository.countByStatusAndAtualizadoEmAfter(
                StatusSolicitation.APROVADA, trintaDiasAtras);
        long rejeitadas30Dias = solicitacaoRepository.countByStatusAndAtualizadoEmAfter(
                StatusSolicitation.REJEITADA, trintaDiasAtras);

        DashboardStatsDto stats = new DashboardStatsDto(
                solicitacoesPendentes,
                totalFuncionarios,
                aprovadas30Dias,
                rejeitadas30Dias);

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

    @GetMapping("/cpf-check")
    public ResponseEntity<Void> verificarCpf(@RequestParam String cpf,
            jakarta.servlet.http.HttpServletRequest request) {
        System.out.println("=== HEADERS NO /cpf-check ===");
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            System.out.println(headerName + ": " + request.getHeader(headerName));
        }
        System.out.println("===========================");

        String cpfFormatado = CpfUtils.formatar(cpf);
        boolean existe = funcionarioRepository.findByCpfExato(cpf, cpfFormatado).isPresent();

        if (existe) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/cpf-buscar")
    public ResponseEntity<UsuarioDto> getFuncionarioByCpf(@RequestParam String cpf) {
        String cpfFormatado = CpfUtils.formatar(cpf);

        return funcionarioRepository.findByCpfExato(cpf, cpfFormatado)
                .map(funcionario -> ResponseEntity.ok(new UsuarioDto(funcionario)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/funcionarios/{id}")
    public ResponseEntity<UsuarioDto> getFuncionario(@PathVariable Long id) {
        return funcionarioRepository.findById(id)
                .map(funcionario -> ResponseEntity.ok(new UsuarioDto(funcionario)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/funcionarios")
    public ResponseEntity<?> adicionarFuncionario(@RequestBody CadastroFuncionarioDto dto) {
        try {
            Funcionario funcionario = new Funcionario();
            atualizarDadosFuncionario(funcionario, dto);
            funcionario.setRole(Role.ROLE_FUNCIONARIO);
            funcionario.setStatus("ativo");
            Funcionario saved = funcionarioRepository.save(funcionario);
            return ResponseEntity.status(HttpStatus.CREATED).body(new UsuarioDto(saved));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("CPF ou matrícula já cadastrado.");
        }
    }

    @PutMapping("/funcionarios/{id}")
    public ResponseEntity<?> atualizarFuncionario(@PathVariable Long id,
            @RequestBody CadastroFuncionarioDto dto) {
        Optional<Funcionario> funcionarioOptional = funcionarioRepository.findById(id);

        if (funcionarioOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Funcionario funcionario = funcionarioOptional.get();
            atualizarDadosFuncionario(funcionario, dto);

            if (dto.status() != null) {
                funcionario.setStatus(dto.status());
            }

            Funcionario saved = funcionarioRepository.save(funcionario);
            return ResponseEntity.ok(new UsuarioDto(saved));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("CPF ou matrícula já cadastrado.");
        }
    }

    @DeleteMapping("/funcionarios/{id}")
    public ResponseEntity<Void> removerFuncionario(@PathVariable Long id) {
        if (!funcionarioRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        funcionarioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void atualizarDadosFuncionario(Funcionario funcionario, CadastroFuncionarioDto dto) {
        funcionario.setNome(dto.nome());
        funcionario.setMatricula(nullIfBlank(dto.matricula()));
        funcionario.setCargo(dto.cargo());
        funcionario.setSetor(nullIfBlank(dto.setor()));
        // CPF e foto: string vazia -> null (evita violação de constraint unique)
        String cpfValido = nullIfBlank(dto.cpf());
        funcionario.setCpf(cpfValido != null ? CpfUtils.formatar(cpfValido) : null);
        funcionario.setFoto(dto.foto() != null && !dto.foto().isEmpty() ? dto.foto() : null);

        if (dto.senha() != null && !dto.senha().isEmpty()) {
            funcionario.setSenha(passwordEncoder.encode(dto.senha()));
        }
    }

    private String nullIfBlank(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    // ─── Solicitações ────────────────────────────────────────────────────────

    @GetMapping("/solicitacoes")
    public ResponseEntity<List<SolicitacaoDto>> getSolicitacoes() {
        List<SolicitacaoDto> solicitacoes = solicitacaoRepository.findAll()
                .stream()
                .map(SolicitacaoDto::new)
                .toList();
        return ResponseEntity.ok(solicitacoes);
    }

    @GetMapping("/solicitacoes/funcionario/{funcionarioId}")
    public ResponseEntity<List<SolicitacaoDto>> getSolicitacoesByFuncionario(@PathVariable Long funcionarioId) {
        if (!funcionarioRepository.existsById(funcionarioId)) {
            return ResponseEntity.notFound().build();
        }
        List<SolicitacaoDto> solicitacoes = solicitacaoRepository.findByFuncionarioId(funcionarioId)
                .stream()
                .map(SolicitacaoDto::new)
                .toList();
        return ResponseEntity.ok(solicitacoes);
    }

    @PatchMapping("/solicitacoes/{id}/status")
    public ResponseEntity<SolicitacaoDto> atualizarStatusSolicitacao(
            @PathVariable Long id,
            @RequestParam StatusSolicitation status) {

        Optional<Solicitacao> optional = solicitacaoRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitacao solicitacao = optional.get();
        solicitacao.setStatus(status);
        Solicitacao saved = solicitacaoRepository.save(solicitacao);
        return ResponseEntity.ok(new SolicitacaoDto(saved));
    }

    @DeleteMapping("/solicitacoes/{id}")
    public ResponseEntity<Void> removerSolicitacao(@PathVariable Long id) {
        if (!solicitacaoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        solicitacaoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}