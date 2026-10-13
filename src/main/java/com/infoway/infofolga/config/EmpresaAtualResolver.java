package com.infoway.infofolga.config;

import com.infoway.infofolga.model.Colaborador;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Diz ao Hibernate de qual empresa é a requisição atual. Com isso, toda consulta às entidades
 * marcadas com @TenantId é filtrada por empresa, e todo insert recebe a empresa do usuário logado.
 */
@Component
public class EmpresaAtualResolver implements CurrentTenantIdentifierResolver<Long>, HibernatePropertiesCustomizer {

    /**
     * Sem usuário autenticado (login e validação do token), as consultas não são filtradas.
     * Só é seguro porque nesses pontos a busca é por CPF, que é único em todo o sistema.
     */
    static final Long SEM_EMPRESA = 0L;

    @Override
    public Long resolveCurrentTenantIdentifier() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Colaborador colaborador) {
            return colaborador.getEmpresaId();
        }
        return SEM_EMPRESA;
    }

    @Override
    public boolean isRoot(Long empresaId) {
        return SEM_EMPRESA.equals(empresaId);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
