package com.infoway.infofolga.repository;

import com.infoway.infofolga.model.Batida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BatidaRepository extends JpaRepository<Batida, Long> {

    List<Batida> findByColaboradorIdAndDataHoraBetweenOrderByDataHora(Long colaboradorId, LocalDateTime inicio,
                                                                       LocalDateTime fim);
}
