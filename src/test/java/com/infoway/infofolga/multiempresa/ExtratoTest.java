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
    private static final String CPF_FUNC_B = "11122233396";
    private static final YearMonth OUTUBRO = YearMonth.of(2026, 10);

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ColaboradorRepository colaboradores;
    @Autowired private ExtratoService extratos;

    private long anaA;
    private long empresaA;
    private long funcB;

    @BeforeEach
    void criarDuasEmpresasComPontos() {
        jdbc.update("DELETE FROM apuracao_dia");
        jdbc.update("DELETE FROM configuracao_pontuacao");
        jdbc.update("DELETE FROM colaboradores");
        jdbc.update("DELETE FROM empresas");
        long a = jdbc.queryForObject("INSERT INTO empresas (nome) VALUES ('Empresa A') RETURNING id", Long.class);
        empresaA = a;
        long b = jdbc.queryForObject("INSERT INTO empresas (nome) VALUES ('Empresa B') RETURNING id", Long.class);
        String sql = "INSERT INTO colaboradores (empresa_id, nome, cpf, email, senha, role, status) VALUES (?, ?, ?, ?, 'x', ?, 'ativo') RETURNING id";
        jdbc.queryForObject(sql, Long.class, a, "RH A", CPF_RH_A, "rh@a.test", "RH");
        anaA = jdbc.queryForObject(sql, Long.class, a, "Ana A", CPF_ANA_A, "ana@a.test", "FUNCIONARIO");
        // Funcionário de outra empresa: tem banco de horas, mas não pode ser visto pela empresa A.
        funcB = jdbc.queryForObject(sql, Long.class, b, "Func B", CPF_FUNC_B, "func@b.test", "FUNCIONARIO");

        dia(a, anaA, "2026-09-30", "2.00", false);
        dia(a, anaA, "2026-10-01", "1.50", false);
        dia(a, anaA, "2026-10-02", "-0.50", false);
        dia(a, anaA, "2026-10-05", "0", true);
        dia(b, funcB, "2026-10-01", "8.00", false);
    }

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void extratoDoMesTrazSoOsDiasDoMesMasOSaldoAcumulado() {
        Colaborador ana = logarComo(CPF_ANA_A);
        var extrato = extratos.meuExtrato(ana, OUTUBRO);

        assertEquals(3, extrato.dias().size());
        assertEquals("2026-10-05", extrato.dias().get(0).data().toString());
        assertIgual("1.00", extrato.saldoDoMes());
        assertIgual("3.00", extrato.saldo());
    }

    @Test
    void equipeMostraSoOsFuncionariosDaEmpresaDeQuemPede() {
        logarComo(CPF_RH_A);
        var equipe = extratos.equipe();

        // O RH não entra: só funcionário tem banco de horas.
        assertEquals(1, equipe.size());
        var ana = equipe.get(0);
        assertEquals(anaA, ana.colaborador().id());
        assertIgual("3.00", ana.saldo());
        assertEquals(1, ana.diasPendentes());
    }

    @Test
    void ceoGerenteERhNaoTemBancoDeHoras() {
        Colaborador rh = logarComo(CPF_RH_A);
        var erro = assertThrows(ResponseStatusException.class, () -> extratos.meuExtrato(rh, OUTUBRO));
        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());

        long idRh = rh.getId();
        var outro = assertThrows(ResponseStatusException.class, () -> extratos.extratoDe(idRh, OUTUBRO));
        assertEquals(HttpStatus.NOT_FOUND, outro.getStatusCode());
    }

    @Test
    void naoAbreOExtratoDeOutraEmpresa() {
        logarComo(CPF_RH_A);
        var erro = assertThrows(ResponseStatusException.class, () -> extratos.extratoDe(funcB, OUTUBRO));
        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }

    @Test
    void rhParticipaQuandoAEmpresaLigaAOpcao() {
        // Empresa A tem o RH como funcionário (na Infoway ele é terceirizado e fica fora).
        jdbc.update("""
                INSERT INTO configuracao_pontuacao (empresa_id, vigente_desde, jornada_seg, jornada_ter, jornada_qua,
                    jornada_qui, jornada_sex, jornada_sab, jornada_dom, tolerancia_min, peso_domingo, peso_feriado,
                    fator_hora_noturna, compensar_falta, politica_sem_saldo, rh_tem_banco_de_horas, criado_em)
                VALUES (?, '2020-01-01', 480, 480, 480, 480, 480, 240, 0, 10, 1, 1, 1.142857142857, false, 'AVISAR', true, NOW())
                """, empresaA);
        Colaborador rh = logarComo(CPF_RH_A);

        assertIgual("0", extratos.meuExtrato(rh, OUTUBRO).saldo());
        assertEquals(2, extratos.equipe().size());
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
