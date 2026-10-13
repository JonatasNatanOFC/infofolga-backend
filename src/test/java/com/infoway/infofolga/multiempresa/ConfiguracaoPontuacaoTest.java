package com.infoway.infofolga.multiempresa;

import com.infoway.infofolga.dto.ConfiguracaoPontuacaoDto;
import com.infoway.infofolga.dto.FeriadoDto;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.pontuacao.PoliticaSemSaldo;
import com.infoway.infofolga.pontuacao.RegrasPontuacao;
import com.infoway.infofolga.repository.ColaboradorRepository;
import com.infoway.infofolga.service.ConfiguracaoPontuacaoService;
import com.infoway.infofolga.service.RegrasPontuacaoService;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "api.security.token.secret=segredo-de-teste-com-mais-de-32-bytes!!"
})
@Testcontainers
class ConfiguracaoPontuacaoTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String CPF_RH_A = "12345678909";
    private static final String CPF_CEO_B = "11122233396";
    private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ColaboradorRepository colaboradores;
    @Autowired private ConfiguracaoPontuacaoService configuracao;
    @Autowired private RegrasPontuacaoService regras;

    @BeforeEach
    void criarDuasEmpresas() {
        jdbc.update("DELETE FROM configuracao_pontuacao");
        jdbc.update("DELETE FROM feriados");
        jdbc.update("DELETE FROM colaboradores");
        jdbc.update("DELETE FROM empresas");
        long a = jdbc.queryForObject("INSERT INTO empresas (nome) VALUES ('Empresa A') RETURNING id", Long.class);
        long b = jdbc.queryForObject("INSERT INTO empresas (nome) VALUES ('Empresa B') RETURNING id", Long.class);
        jdbc.update("INSERT INTO colaboradores (empresa_id, nome, cpf, email, senha, role, status) VALUES (?, 'RH A', ?, 'rh@a.test', 'x', 'RH', 'ativo')", a, CPF_RH_A);
        jdbc.update("INSERT INTO colaboradores (empresa_id, nome, cpf, email, senha, role, status) VALUES (?, 'CEO B', ?, 'ceo@b.test', 'x', 'CEO', 'ativo')", b, CPF_CEO_B);
    }

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void regrasSalvasValemSoParaAEmpresaDeQuemSalvou() {
        Colaborador rh = logarComo(CPF_RH_A);
        configuracao.salvar(regrasPadrao(HOJE, "1.5"), rh);

        RegrasPontuacao vigente = regras.regrasEm(HOJE).orElseThrow();
        assertEquals(0, new BigDecimal("1.5").compareTo(vigente.pesoDomingo()));
        assertEquals("RH A", configuracao.historico().get(0).alteradoPor());

        logarComo(CPF_CEO_B);
        assertTrue(configuracao.historico().isEmpty());
        assertTrue(regras.regrasEm(HOJE).isEmpty(), "pontuação da empresa B continua desligada");
    }

    @Test
    void naoAceitaRegrasValendoNoPassado() {
        Colaborador rh = logarComo(CPF_RH_A);
        var erro = assertThrows(ResponseStatusException.class,
                () -> configuracao.salvar(regrasPadrao(HOJE.minusDays(1), "1"), rh));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void mesmaDataSubstituiAVersaoEmVezDeDuplicar() {
        Colaborador rh = logarComo(CPF_RH_A);
        configuracao.salvar(regrasPadrao(HOJE.plusDays(5), "1"), rh);
        configuracao.salvar(regrasPadrao(HOJE.plusDays(5), "2"), rh);

        List<ConfiguracaoPontuacaoDto> historico = configuracao.historico();
        assertEquals(1, historico.size());
        assertEquals(0, new BigDecimal("2").compareTo(historico.get(0).pesoDomingo()));
    }

    @Test
    void feriadoDaEmpresaEntraNoCalendarioSoDela() {
        logarComo(CPF_RH_A);
        LocalDate festa = LocalDate.of(HOJE.getYear(), 7, 26);
        configuracao.adicionarFeriado(new FeriadoDto(null, festa, "Festa da padroeira", false));

        assertTrue(regras.ehFeriado(festa));
        assertTrue(configuracao.feriadosDoAno(HOJE.getYear()).stream().anyMatch(f -> f.data().equals(festa) && !f.nacional()));

        var natal = new FeriadoDto(null, LocalDate.of(HOJE.getYear(), 12, 25), "Natal de novo", false);
        var erro = assertThrows(ResponseStatusException.class, () -> configuracao.adicionarFeriado(natal));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());

        logarComo(CPF_CEO_B);
        assertFalse(regras.ehFeriado(festa));
    }

    private Colaborador logarComo(String cpf) {
        SecurityContextHolder.clearContext();
        Colaborador c = colaboradores.findByCpf(cpf).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(c, null, c.getAuthorities()));
        return c;
    }

    private static ConfiguracaoPontuacaoDto regrasPadrao(LocalDate inicio, String pesoDomingo) {
        return new ConfiguracaoPontuacaoDto(null, inicio, 480, 480, 480, 480, 480, 240, 0, 10,
                new BigDecimal(pesoDomingo), BigDecimal.ONE, RegrasPontuacao.HORA_NOTURNA_CLT, false,
                PoliticaSemSaldo.AVISAR, null, null, null);
    }
}
