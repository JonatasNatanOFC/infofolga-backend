package com.infoway.infofolga.repository;

import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    Optional<Funcionario> findByCpf(String cpf);
}