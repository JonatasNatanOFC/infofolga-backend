package com.infoway.infofolga.repository;

import com.infoway.infofolga.model.Feriado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FeriadoRepository extends JpaRepository<Feriado, Long> {

    boolean existsByData(LocalDate data);

    List<Feriado> findByDataBetweenOrderByData(LocalDate inicio, LocalDate fim);
}
