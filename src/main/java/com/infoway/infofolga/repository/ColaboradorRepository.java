package com.infoway.infofolga.repository;

import com.infoway.infofolga.model.Colaborador;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ColaboradorRepository extends JpaRepository<Colaborador, Long> {
    Optional<Colaborador> findByCpf(String cpf);
}