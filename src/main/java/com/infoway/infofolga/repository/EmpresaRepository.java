package com.infoway.infofolga.repository;

import com.infoway.infofolga.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
}
