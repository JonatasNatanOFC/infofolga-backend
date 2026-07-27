package com.infoway.infofolga.repository;

import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {

    List<Solicitacao> findByColaboradorId(Long id);

    @Query("SELECT s FROM Solicitacao s JOIN FETCH s.colaborador LEFT JOIN FETCH s.aprovador ORDER BY s.id DESC")
    List<Solicitacao> findAllOtimizado();

    @Query("SELECT s FROM Solicitacao s JOIN FETCH s.colaborador LEFT JOIN FETCH s.aprovador WHERE s.colaborador.id = :id ORDER BY s.id DESC")
    List<Solicitacao> findByColaboradorIdOtimizado(@Param("id") Long id);

    long countByStatusAndAtualizadoEmAfter(StatusSolicitation status, LocalDateTime data);

    @Query("SELECT COUNT(s) FROM Solicitacao s WHERE s.status IN :statusList AND :hoje BETWEEN s.dataInicio AND s.dataFim")
    long countFolgasAtivasHoje(@Param("statusList") List<StatusSolicitation> statusList, @Param("hoje") LocalDate hoje);

    @Query("SELECT COUNT(s) FROM Solicitacao s WHERE s.status = :status AND s.dataInicio > :hoje AND s.dataInicio <= :daquiA7Dias")
    long countFolgasProximosDias(@Param("status") StatusSolicitation status, @Param("hoje") LocalDate hoje, @Param("daquiA7Dias") LocalDate daquiA7Dias);
}