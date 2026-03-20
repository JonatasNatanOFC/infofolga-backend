package com.infoway.infofolga.controller;

import com.infoway.infofolga.dto.CadastroFuncionarioDto;
import com.infoway.infofolga.dto.DashboardStatsDto;
import com.infoway.infofolga.dto.UsuarioDto;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Role;
import com.infoway.infofolga.repository.FuncionarioRepository;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import com.infoway.infofolga.model.StatusSolicitation;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gerencia")
public class GerenciaController {

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private SolicitacaoRepository solicitacaoRepository;

    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasRole('ROLE_GERENTE')")
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
    @PreAuthorize("hasRole('ROLE_GERENTE')")
    public ResponseEntity<List<UsuarioDto>> getFuncionarios() {
        List<UsuarioDto> funcionarios = funcionarioRepository
                .findAllByRole(Role.ROLE_FUNCIONARIO)
                .stream()
                .map(UsuarioDto::new)
                .toList();
        return ResponseEntity.ok(funcionarios);
    }


    @GetMapping("/funcionarios/{id}")
    @PreAuthorize("hasRole('ROLE_GERENTE')")
    public ResponseEntity<UsuarioDto> getFuncionario(@PathVariable Long id) {
        Funcionario funcionario = funcionarioRepository.findById(id).orElseThrow();
        return ResponseEntity.ok(new UsuarioDto(funcionario));
    }

    @PostMapping("/funcionarios")
    @PreAuthorize("hasRole('ROLE_GERENTE')")
    public ResponseEntity<UsuarioDto> adicionarFuncionario(@RequestBody CadastroFuncionarioDto dto) {
        Funcionario funcionario = new Funcionario();
        funcionario.setNome(dto.nome());
        funcionario.setMatricula(dto.matricula());
        funcionario.setCargo(dto.cargo());
        funcionario.setSetor(dto.setor());
        funcionario.setCpf(dto.cpf());
        funcionario.setSenha(new BCryptPasswordEncoder().encode(dto.senha()));
        funcionario.setFoto(dto.foto());
        funcionario.setRole(Role.ROLE_FUNCIONARIO);
        funcionario.setStatus("ativo");
        Funcionario saved = funcionarioRepository.save(funcionario);
        return ResponseEntity.ok(new UsuarioDto(saved));
    }


    @PutMapping("/funcionarios/{id}")
    @PreAuthorize("hasRole('ROLE_GERENTE')")
    public ResponseEntity<UsuarioDto> atualizarFuncionario(@PathVariable Long id, @RequestBody CadastroFuncionarioDto dto) {
        Funcionario funcionario = funcionarioRepository.findById(id).orElseThrow();
        funcionario.setNome(dto.nome());
        funcionario.setMatricula(dto.matricula());
        funcionario.setCargo(dto.cargo());
        funcionario.setSetor(dto.setor());
        funcionario.setCpf(dto.cpf());
        funcionario.setFoto(dto.foto());
        if (dto.status() != null) funcionario.setStatus(dto.status());
        if (dto.senha() != null && !dto.senha().isEmpty()) {
            funcionario.setSenha(new BCryptPasswordEncoder().encode(dto.senha()));
        }
        Funcionario saved = funcionarioRepository.save(funcionario);
        return ResponseEntity.ok(new UsuarioDto(saved));
    }

    @DeleteMapping("/funcionarios/{id}")
    @PreAuthorize("hasRole('ROLE_GERENTE')")
    public ResponseEntity<Void> removerFuncionario(@PathVariable Long id) {
        funcionarioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}