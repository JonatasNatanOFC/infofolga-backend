package com.infoway.infofolga.model;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerfilRhTest {

    private static List<String> permissoes(Role role) {
        Colaborador c = new Colaborador();
        c.setRole(role);
        return c.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList();
    }

    @Test
    void rhPedeFolgasComoFuncionarioMasNaoAprova() {
        // Sem ROLE_GERENTE: os endpoints de aprovação continuam fechados para o RH.
        assertEquals(List.of("ROLE_FUNCIONARIO", "ROLE_RH"), permissoes(Role.RH));
    }

    @Test
    void demaisPerfisNaoMudaram() {
        assertEquals(List.of("ROLE_CEO", "ROLE_FUNCIONARIO", "ROLE_GERENTE"), permissoes(Role.CEO));
        assertEquals(List.of("ROLE_FUNCIONARIO", "ROLE_GERENTE"), permissoes(Role.GERENTE));
        assertEquals(List.of("ROLE_FUNCIONARIO"), permissoes(Role.FUNCIONARIO));
    }
}
