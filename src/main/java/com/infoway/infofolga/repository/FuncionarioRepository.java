package com.infoway.infofolga.repository;

import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    UserDetails findByCpf(String cpf);
    List<Funcionario> findAllByRole(Role role);

    @Query("SELECT f FROM Funcionario f WHERE f.cpf = :cpf OR f.cpf = :cpfFormatado")
    Optional<Funcionario> findByCpfExato(@Param("cpf") String cpf, @Param("cpfFormatado") String cpfFormatado);
}