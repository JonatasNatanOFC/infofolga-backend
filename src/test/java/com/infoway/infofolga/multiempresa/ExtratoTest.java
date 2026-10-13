package com.infoway.infofolga.multiempresa;

import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.repository.ColaboradorRepository;
import com.infoway.infofolga.service.ExtratoService;
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

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "api.security.token.secret=segredo-de-teste-com-mais-de-32-bytes!!"
})
@Testcontainers
class ExtratoTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String CPF_RH_A = "12345678909";
    private static final String CPF_ANA_A = "98765432100";
    private static final String CPF_CEO_B = "11122233396";
    private static final YearMonth OUTUBRO = YearMonth.of(2026, 10);

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ColaboradorRepository colaboradores;
    @Autowired private ExtratoService extratos;

    private long anaA;
    private long ceoB;

    @BeforeEach
    void criarDuasEmpresasComPontos() {
        jdbc.update("DELETE FROM apuracao_dia");
        jdbc.update("DELETE FROM colaboradores");
        jdbc.update("DELETE FROM empresas");
        long a = jdbc.queryForObject("INSERT INTO empresas (nome) VALUES ('Empresa A') RETURNING id", Long.class);
        long b = jdbc.queryForObject("INSERT INTO empresas (nome) VALUES ('Empresa B') RETURNING id", Long.class);
        String sql = "INSERT INTO colaboradores (empresa_id, nome, cpf, email, senha, role, status) VALUES (?, ?, ?, ?, 'x', ?, 'ativo') RETURNING id";
        jdbc.queryForObject(sql, Long.class, a, "RH A", CPF_RH_A, "rh@a.test", "RH");
        anaA = jdbc.queryForObject(sql, Long.class, a, "Ana A", CPF_ANA_A, "ana@a.test", "FUNCIONARIO");
        ceoB = jdbc.queryForObject(sql, Long.class, b, "CEO B", CPF_CEO_B, "ceo@b.test", "CEO");

        dia(a, anaA, "2026-09-30", "2.00", false);
        dia(a, anaA, "2026-10-01", "1.50", false);
        dia(a, anaA, "2026-10-02", "-0.50", false);
        dia(a, anaA, "2026-10-05", "0", true);
        dia(b, ceoB, "2026-10-01", "8.00", false);
    }

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void extratoDoMesTrazSoOsDiasDoMesMasOSaldoAcumulado() {
        Colaborador ana = logarComo(CPF_ANA_A);
        var extrato = extratos.extrato(ana, OUTUBRO);

        assertEquals(3, extrato.dias().size());
        assertEquals("2026-10-05", extrato.dias().get(0).data().toString());
        assertIgual("1.00", extrato.saldoDoMes());
        assertIgual("3.00", extrato.saldo());
    }

    @Test
    void equipeMostraSoAEmpresaDeQuemPede() {
        logarComo(CPF_RH_A);
        var equipe = extratos.equipe();

        assertEquals(2, equipe.size());
        var ana = equipe.stream().filter(s -> s.colaborador().id() == anaA).findFirst().orElseThrow();
        assertIgual("3.00", ana.saldo());
        assertEquals(1, ana.diasPendentes());
        var rh = equipe.stream().filter(s -> !s.colaborador().id().equals(anaA)).findFirst().orElseThrow();
        assertIgual("0", rh.saldo());
    }

    @Test
    void naoAbreOExtratoDeOutraEmpresa() {
        logarComo(CPF_RH_A);
        var erro = assertThrows(ResponseStatusException.class, () -> extratos.extratoDe(ceoB, OUTUBRO));
        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }

    private void dia(long empresa, long colaborador, String data, String pontos, boolean inconsistente) {
        jdbc.update("""
                INSERT INTO apuracao_dia (empresa_id, colaborador_id, data, situacao, segundos_trabalhados,
                    segundos_previstos, pontos, inconsistente, descricao, atualizado_em)
                VALUES (?, ?, ?::date, 'NORMAL', 0, 28800, ?::numeric, ?, 'teste', NOW())
                """, empresa, colaborador, data, pontos, inconsistente);
    }

    private Colaborador logarComo(String cpf) {
        SecurityContextHolder.clearContext();
        Colaborador c = colaboradores.findByCpf(cpf).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(c, null, c.getAuthorities()));
        return c;
    }

    private static void assertIgual(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), () -> "esperado " + esperado + ", veio " + real);
    }
}
