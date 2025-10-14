package com.infoway.infofolga.repository;

import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    UserDetails findByMatricula(String matricula);

    // Método útil para buscar funcionários por seu papel
    List<Funcionario> findAllByRole(Role role);
}