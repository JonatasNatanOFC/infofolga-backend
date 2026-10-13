package com.infoway.infofolga.multiempresa;

import com.infoway.infofolga.dto.CadastroColaboradorDto;
import com.infoway.infofolga.dto.UsuarioDto;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.model.Role;
import com.infoway.infofolga.repository.ColaboradorRepository;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import com.infoway.infofolga.service.ColaboradorService;
import com.infoway.infofolga.service.DashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Garante que um usuário nunca enxerga nem altera dados de outra empresa.
 * Roda contra um PostgreSQL de verdade (Docker): o filtro do @TenantId acontece no SQL gerado pelo Hibernate.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "api.security.token.secret=segredo-de-teste-com-mais-de-32-bytes!!"
})
@Testcontainers
class IsolamentoEntreEmpresasTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    // CPFs fictícios válidos.
    private static final String CPF_CEO_A = "12345678909";
    private static final String CPF_FUNC_A = "98765432100";
    private static final String CPF_CEO_B = "11122233396";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ColaboradorRepository colaboradores;
    @Autowired private SolicitacaoRepository solicitacoes;
    @Autowired private ColaboradorService colaboradorService;
    @Autowired private DashboardService dashboardService;

    private long empresaA;
    private long empresaB;
    private long idCeoB;

    @BeforeEach
    void criarDuasEmpresas() {
        jdbc.update("DELETE FROM solicitacoes");
        jdbc.update("DELETE FROM colaboradores");
        jdbc.update("DELETE FROM empresas");
        // Dados inseridos por SQL puro, sem passar pelo filtro do Hibernate.
        empresaA = jdbc.queryForObject("INSERT INTO empresas (nome) VALUES ('Empresa A') RETURNING id", Long.class);
        empresaB = jdbc.queryForObject("INSERT INTO empresas (nome) VALUES ('Empresa B') RETURNING id", Long.class);
        inserirColaborador(empresaA, "CEO A", CPF_CEO_A, "CEO");
        long idFuncA = inserirColaborador(empresaA, "Func A", CPF_FUNC_A, "FUNCIONARIO");
        idCeoB = inserirColaborador(empresaB, "CEO B", CPF_CEO_B, "CEO");
        inserirSolicitacao(empresaA, idFuncA);
        inserirSolicitacao(empresaB, idCeoB);
        inserirSolicitacao(empresaB, idCeoB);
    }

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void semUsuarioLogadoOLoginEncontraQualquerEmpresaPeloCpf() {
        assertTrue(colaboradores.findByCpf(CPF_CEO_B).isPresent());
    }

    @Test
    void listagensTrazemSoAEmpresaDoUsuario() {
        logarComo(CPF_CEO_A);
        assertEquals(2, colaboradores.findAll().size());
        assertEquals(1, solicitacoes.findAllOtimizado().size());
        assertEquals(2, dashboardService.getStats(empresaA).totalColaboradores());
    }

    @Test
    void buscaPorIdNaoAlcancaOutraEmpresa() {
        logarComo(CPF_CEO_A);
        assertTrue(colaboradores.findById(idCeoB).isEmpty());
        var erro = assertThrows(ResponseStatusException.class, () -> colaboradorService.buscarPorId(idCeoB));
        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }

    @Test
    void novoColaboradorEntraNaEmpresaDeQuemCadastrou() {
        logarComo(CPF_CEO_A);
        UsuarioDto novo = colaboradorService.cadastrar(new CadastroColaboradorDto(
                "Novo A", "44455566619", "novo@a.test", "senha-forte-1", null, null, null, Role.FUNCIONARIO));
        Long empresaGravada = jdbc.queryForObject(
                "SELECT empresa_id FROM colaboradores WHERE id = ?", Long.class, novo.id());
        assertEquals(empresaA, empresaGravada);
    }

    @Test
    void cpfDeOutraEmpresaNaoPodeSerCadastradoDeNovo() {
        logarComo(CPF_CEO_A);
        var erro = assertThrows(ResponseStatusException.class, () -> colaboradorService.cadastrar(new CadastroColaboradorDto(
                "Duplicado", CPF_CEO_B, "dup@a.test", "senha-forte-1", null, null, null, Role.FUNCIONARIO)));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    private void logarComo(String cpf) {
        Colaborador c = colaboradores.findByCpf(cpf).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(c, null, c.getAuthorities()));
    }

    private long inserirColaborador(long empresa, String nome, String cpf, String role) {
        return jdbc.queryForObject("INSERT INTO colaboradores (empresa_id, nome, cpf, email, senha, role, status) "
                + "VALUES (?, ?, ?, ?, 'x', ?, 'ativo') RETURNING id", Long.class, empresa, nome, cpf, cpf + "@t.test", role);
    }

    private void inserirSolicitacao(long empresa, long colaborador) {
        jdbc.update("INSERT INTO solicitacoes (empresa_id, colaborador_id, tipo, status, data_inicio, data_fim, criado_em, versao) "
                + "VALUES (?, ?, 'FOLGA', 'PENDENTE', CURRENT_DATE, CURRENT_DATE, NOW(), 0)", empresa, colaborador);
    }
}
