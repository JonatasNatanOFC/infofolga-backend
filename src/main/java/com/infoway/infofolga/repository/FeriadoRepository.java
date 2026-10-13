package com.infoway.infofolga.repository;

import com.infoway.infofolga.model.Feriado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FeriadoRepository extends JpaRepository<Feriado, Long> {

    /** Feriado da empresa na data: cadastrado nela, ou anual no mesmo dia e mês desde um ano anterior. */
    default boolean existeEm(LocalDate data) {
        return contarEm(data, data.getMonthValue(), data.getDayOfMonth()) > 0;
    }

    /** Algum feriado que cairia no mesmo dia e mês de um novo feriado anual, do ano dele em diante. */
    default boolean conflitaComAnual(LocalDate desde) {
        return contarMesmoDiaDesde(desde, desde.getMonthValue(), desde.getDayOfMonth()) > 0;
    }

    /** Candidatos para o ano: os avulsos do ano e todos os anuais que começaram até o fim dele. */
    @Query("select f from Feriado f where (f.anual = false and f.data between :inicio and :fim) or (f.anual = true and f.data <= :fim)")
    List<Feriado> candidatosEntre(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("""
            select count(f) from Feriado f
            where f.data = :data
               or (f.anual = true and f.data <= :data
                   and extract(month from f.data) = :mes and extract(day from f.data) = :dia)
            """)
    long contarEm(@Param("data") LocalDate data, @Param("mes") int mes, @Param("dia") int dia);

    @Query("""
            select count(f) from Feriado f
            where extract(month from f.data) = :mes and extract(day from f.data) = :dia
              and (f.anual = true or f.data >= :desde)
            """)
    long contarMesmoDiaDesde(@Param("desde") LocalDate desde, @Param("mes") int mes, @Param("dia") int dia);
}
