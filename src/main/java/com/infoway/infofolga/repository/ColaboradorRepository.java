package com.infoway.infofolga.repository;

import com.infoway.infofolga.model.Colaborador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ColaboradorRepository extends JpaRepository<Colaborador, Long> {
    Optional<Colaborador> findByCpf(String cpf);

    // CPF e e-mail são únicos no sistema todo (o CPF identifica a empresa no login), mas o @TenantId
    // filtraria a checagem pela empresa atual. SQL nativo não passa por esse filtro.
    @Query(value = "SELECT EXISTS (SELECT 1 FROM colaboradores WHERE cpf = :cpf)", nativeQuery = true)
    boolean cpfJaCadastrado(@Param("cpf") String cpf);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM colaboradores WHERE LOWER(email) = LOWER(:email))", nativeQuery = true)
    boolean emailJaCadastrado(@Param("email") String email);
}
