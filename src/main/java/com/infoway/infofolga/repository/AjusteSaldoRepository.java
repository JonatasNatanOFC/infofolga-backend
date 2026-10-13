package com.infoway.infofolga.repository;

import com.infoway.infofolga.model.AjusteSaldo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface AjusteSaldoRepository extends JpaRepository<AjusteSaldo, Long> {

    List<AjusteSaldo> findByColaboradorIdAndDataBetweenOrderByDataDesc(Long colaboradorId, LocalDate inicio,
                                                                        LocalDate fim);

    @Query("SELECT COALESCE(SUM(a.pontos), 0) FROM AjusteSaldo a WHERE a.colaboradorId = :colaboradorId")
    BigDecimal somaDe(@Param("colaboradorId") Long colaboradorId);

    @Query("SELECT a.colaboradorId AS colaboradorId, SUM(a.pontos) AS soma FROM AjusteSaldo a GROUP BY a.colaboradorId")
    List<SomaPorColaborador> somasPorColaborador();

    interface SomaPorColaborador {
        Long getColaboradorId();

        BigDecimal getSoma();
    }
}
