package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.CadastroColaboradorDto;
import com.infoway.infofolga.dto.CriarSolicitacaoDto;
import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.model.Role;
import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.model.TipoSolicitacao;
import com.infoway.infofolga.repository.ColaboradorRepository;
import com.infoway.infofolga.repository.SolicitacaoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegrasDeNegocioTest {

    private final SolicitacaoRepository solicitacaoRepository = mock(SolicitacaoRepository.class);
    private final ColaboradorRepository colaboradorRepository = mock(ColaboradorRepository.class);
    private final SolicitacaoService solicitacaoService =
            new SolicitacaoService(solicitacaoRepository, colaboradorRepository);
    private final ColaboradorService colaboradorService =
            new ColaboradorService(colaboradorRepository, mock(PasswordEncoder.class));

    private static final LocalDate HOJE = LocalDate.now();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private static Colaborador colaborador(long id, Role role) {
        Colaborador c = new Colaborador();
        c.setId(id);
        c.setRole(role);
        c.setStatus("ativo");
        return c;
    }

    private static void logarComo(Colaborador c) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(c, null, c.getAuthorities()));
    }

    private static HttpStatus status(ResponseStatusException e) {
        return HttpStatus.valueOf(e.getStatusCode().value());
    }

    private void criar(TipoSolicitacao tipo, LocalDate inicio, LocalDate fim, HttpStatus esperado) {
        var e = assertThrows(ResponseStatusException.class,
                () -> solicitacaoService.criarSolicitacao(new CriarSolicitacaoDto(tipo, inicio, fim, null)));
        assertEquals(esperado, status(e));
    }

    @Test
    void gerenteNaoAvaliaAPropriaSolicitacao() {
        Colaborador gerente = colaborador(1, Role.GERENTE);
        Solicitacao sol = new Solicitacao();
        sol.setColaborador(gerente);
        sol.setStatus(StatusSolicitation.PENDENTE);
        when(solicitacaoRepository.findById(10L)).thenReturn(Optional.of(sol));
        when(colaboradorRepository.findById(1L)).thenReturn(Optional.of(gerente));

        var e = assertThrows(ResponseStatusException.class, () -> solicitacaoService.aprovarSolicitacao(10L, 1L));
        assertEquals(HttpStatus.FORBIDDEN, status(e));
    }

    @Test
    void regrasDeAntecedenciaEDuracao() {
        logarComo(colaborador(1, Role.FUNCIONARIO));

        criar(TipoSolicitacao.FOLGA, HOJE.minusDays(1), HOJE.minusDays(1), HttpStatus.BAD_REQUEST); // passado
        criar(TipoSolicitacao.FOLGA, HOJE.plusDays(2), HOJE.plusDays(2), HttpStatus.BAD_REQUEST);   // < 3 dias
        criar(TipoSolicitacao.FOLGA, HOJE.plusDays(31), HOJE.plusDays(31), HttpStatus.BAD_REQUEST); // > 30 dias
        criar(TipoSolicitacao.FOLGA, HOJE.plusDays(5), HOJE.plusDays(6), HttpStatus.BAD_REQUEST);   // mais de 1 dia
        criar(TipoSolicitacao.FERIAS, HOJE.plusDays(14), HOJE.plusDays(20), HttpStatus.BAD_REQUEST); // < 15 dias
    }

    @Test
    void periodoSobrepostoEhRecusado() {
        logarComo(colaborador(1, Role.FUNCIONARIO));
        when(solicitacaoRepository.existeSobreposicao(anyLong(), any(), any(), any())).thenReturn(true);

        criar(TipoSolicitacao.FOLGA, HOJE.plusDays(5), HOJE.plusDays(5), HttpStatus.CONFLICT);
    }

    @Test
    void ceoNaoAlteraOutroCeo() {
        logarComo(colaborador(1, Role.CEO));
        when(colaboradorRepository.findById(2L)).thenReturn(Optional.of(colaborador(2, Role.CEO)));
        var dto = new CadastroColaboradorDto(null, null, null, "novaSenha123", null, null, null, null);

        assertEquals(HttpStatus.FORBIDDEN, status(assertThrows(ResponseStatusException.class,
                () -> colaboradorService.atualizar(2L, dto))));
        assertEquals(HttpStatus.FORBIDDEN, status(assertThrows(ResponseStatusException.class,
                () -> colaboradorService.inativar(2L))));
        assertEquals(HttpStatus.FORBIDDEN, status(assertThrows(ResponseStatusException.class,
                () -> colaboradorService.deletar(2L))));
    }
}
