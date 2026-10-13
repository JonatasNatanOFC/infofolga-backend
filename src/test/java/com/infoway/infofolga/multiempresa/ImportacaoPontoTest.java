package com.infoway.infofolga.multiempresa;

import com.infoway.infofolga.dto.AjusteSaldoDto;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.pontuacao.SituacaoDia;
import com.infoway.infofolga.repository.ColaboradorRepository;
import com.infoway.infofolga.service.ExtratoService;
import com.infoway.infofolga.service.ImportacaoPontoService;
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

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "api.security.token.secret=segredo-de-teste-com-mais-de-32-bytes!!"
})
@Testcontainers
class ImportacaoPontoTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    // CPFs fictícios.
    private static final String CPF_RH_A = "12345678909";
    private static final String CPF_ANA_A = "98765432100";
    private static final String CPF_FUNC_B = "11122233396";
    private static final YearMonth SETEMBRO = YearMonth.of(2026, 9);

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ColaboradorRepository colaboradores;
    @Autowired private ImportacaoPontoService importacao;
    @Autowired private ExtratoService extratos;

    private long anaA;

    @BeforeEach
    void criarDuasEmpresas() {
        for (String tabela : List.of("ajustes_saldo", "batidas", "apuracao_dia", "solicitacoes",
                "configuracao_pontuacao", "colaboradores", "empresas")) {
            jdbc.update("DELETE FROM " + tabela);
        }
        long a = jdbc.queryForObject("INSERT INTO empresas (nome) VALUES ('Empresa A') RETURNING id", Long.class);
        long b = jdbc.queryForObject("INSERT INTO empresas (nome) VALUES ('Empresa B') RETURNING id", Long.class);
        String sql = "INSERT INTO colaboradores (empresa_id, nome, cpf, email, senha, role, status) VALUES (?, ?, ?, ?, 'x', ?, 'ativo') RETURNING id";
        jdbc.queryForObject(sql, Long.class, a, "RH A", CPF_RH_A, "rh@a.test", "RH");
        anaA = jdbc.queryForObject(sql, Long.class, a, "Ana A", CPF_ANA_A, "ana@a.test", "FUNCIONARIO");
        jdbc.queryForObject(sql, Long.class, b, "Func B", CPF_FUNC_B, "func@b.test", "FUNCIONARIO");
        jdbc.update("""
                INSERT INTO configuracao_pontuacao (empresa_id, vigente_desde, jornada_seg, jornada_ter, jornada_qua,
                    jornada_qui, jornada_sex, jornada_sab, jornada_dom, tolerancia_min, peso_domingo, peso_feriado,
                    fator_hora_noturna, compensar_falta, politica_sem_saldo, rh_tem_banco_de_horas, criado_em)
                VALUES (?, '2020-01-01', 480, 480, 480, 480, 480, 240, 0, 10, 1, 1, 1.142857142857, false, 'AVISAR', false, NOW())
                """, a);
        // Folga do banco de horas aprovada na sexta 18/09.
        jdbc.update("""
                INSERT INTO solicitacoes (empresa_id, colaborador_id, tipo, status, data_inicio, data_fim, criado_em,
                    versao, motivo_folga, desconta_saldo)
                VALUES (?, ?, 'FOLGA', 'APROVADA', '2026-09-18', '2026-09-18', NOW(), 0, 'BANCO_DE_HORAS', true)
                """, a, anaA);
    }

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    private static String marcacao(int nsr, String dataHora, String cpf) {
        return String.format("%09d3%s:00-0300%s0000", nsr, dataHora, "0" + cpf);
    }

    private static ByteArrayInputStream afd(String... linhas) {
        return new ByteArrayInputStream(String.join("\r\n", linhas).getBytes(StandardCharsets.ISO_8859_1));
    }

    private ByteArrayInputStream arquivoDeSetembro() {
        return afd(
                marcacao(1, "2026-09-16T08:00", CPF_ANA_A), marcacao(2, "2026-09-16T12:00", CPF_ANA_A),
                marcacao(3, "2026-09-16T13:00", CPF_ANA_A), marcacao(4, "2026-09-16T18:00", CPF_ANA_A), // +1h
                marcacao(5, "2026-09-17T08:00", CPF_ANA_A),                                              // sem saída
                marcacao(8, "2026-09-19T08:00", CPF_ANA_A), marcacao(9, "2026-09-19T12:00", CPF_ANA_A), // sábado 4h
                marcacao(6, "2026-09-16T08:00", CPF_FUNC_B),                                             // outra empresa
                marcacao(7, "2026-09-16T08:00", "52998224725"));                                         // sem cadastro
    }

    @Test
    void importaCalculaEIgnoraQuemNaoEDaEmpresa() {
        logarComo(CPF_RH_A);
        var resultado = importacao.importarAfd(arquivoDeSetembro());

        assertEquals(9, resultado.marcacoes());
        assertEquals(7, resultado.novas());
        assertEquals(List.of("111.***.***-96", "529.***.***-25"), resultado.cpfsNaoEncontrados());

        var extrato = extratos.extratoDe(anaA, SETEMBRO);
        var dias = extrato.dias();
        assertEquals(4, dias.size()); // 16 (+1), 17 (batida faltando), 18 (folga, sem batida) e 19 (sábado exato)
        assertIgual("1.00", dias.stream().filter(d -> d.data().getDayOfMonth() == 16).findFirst().orElseThrow().pontos());
        assertTrue(dias.stream().filter(d -> d.data().getDayOfMonth() == 17).findFirst().orElseThrow().inconsistente());
        var folga = dias.stream().filter(d -> d.data().getDayOfMonth() == 18).findFirst().orElseThrow();
        assertEquals(SituacaoDia.FOLGA_COM_DESCONTO, folga.situacao());
        assertIgual("-8.00", folga.pontos());
        assertIgual("-7.00", extrato.saldo());
    }

    @Test
    void importarOMesmoArquivoDeNovoNaoDuplica() {
        logarComo(CPF_RH_A);
        importacao.importarAfd(arquivoDeSetembro());
        var segunda = importacao.importarAfd(arquivoDeSetembro());

        assertEquals(0, segunda.novas());
        assertIgual("-7.00", extratos.extratoDe(anaA, SETEMBRO).saldo());
    }

    @Test
    void batidaQueFaltavaChegaDepoisECorrigeODia() {
        logarComo(CPF_RH_A);
        importacao.importarAfd(arquivoDeSetembro());
        importacao.importarAfd(afd(marcacao(10, "2026-09-17T17:00", CPF_ANA_A))); // RH corrigiu no relógio

        var dia17 = extratos.extratoDe(anaA, SETEMBRO).dias().stream()
                .filter(d -> d.data().getDayOfMonth() == 17).findFirst().orElseThrow();
        assertFalse(dia17.inconsistente());
        assertIgual("1.00", dia17.pontos()); // 9h corridas, sem almoço registrado
    }

    @Test
    void arquivoSemMarcacaoExplicaOProblema() {
        logarComo(CPF_RH_A);
        var erro = assertThrows(ResponseStatusException.class,
                () -> importacao.importarAfd(afd("000000001" + "3" + "16092026" + "0800" + "012345678901")));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        assertTrue(erro.getReason().contains("Portaria 1510"));
    }

    @Test
    void ajusteEntraNoSaldoMasNinguemAjustaOProprio() {
        Colaborador rh = logarComo(CPF_RH_A);
        var ajuste = new AjusteSaldoDto(null, LocalDate.of(2026, 9, 1), new BigDecimal("12.50"),
                "Saldo trazido do sistema anterior", null, null);
        extratos.lancarAjuste(anaA, ajuste, rh);

        var extrato = extratos.extratoDe(anaA, SETEMBRO);
        assertIgual("12.50", extrato.saldo());
        assertEquals("RH A", extrato.ajustes().get(0).lancadoPor());
        assertIgual("12.50", extratos.equipe().get(0).saldo());

        Colaborador ana = logarComo(CPF_ANA_A);
        var erro = assertThrows(ResponseStatusException.class, () -> extratos.lancarAjuste(anaA, ajuste, ana));
        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
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
