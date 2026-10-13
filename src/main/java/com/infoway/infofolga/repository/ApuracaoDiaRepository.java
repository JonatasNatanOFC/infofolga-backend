package com.infoway.infofolga.repository;

import com.infoway.infofolga.model.ApuracaoDia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ApuracaoDiaRepository extends JpaRepository<ApuracaoDia, Long> {

    Optional<ApuracaoDia> findByColaboradorIdAndData(Long colaboradorId, LocalDate data);

    /** Extrato do colaborador, do dia mais recente para o mais antigo. */
    List<ApuracaoDia> findByColaboradorIdAndDataBetweenOrderByDataDesc(Long colaboradorId, LocalDate inicio, LocalDate fim);

    @Query("SELECT COALESCE(SUM(a.pontos), 0) FROM ApuracaoDia a WHERE a.colaboradorId = :colaboradorId")
    BigDecimal saldoDe(@Param("colaboradorId") Long colaboradorId);
}
