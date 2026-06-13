package com.infoway.infofolga.repository;

import com.infoway.infofolga.model.Gerente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GerenteRepository extends JpaRepository<Gerente, Long> {
    Optional<Gerente> findByCpf(String cpf);
}