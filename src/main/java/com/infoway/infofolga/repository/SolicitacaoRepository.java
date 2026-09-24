package com.infoway.infofolga.repository;

import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import com.infoway.infofolga.model.TipoSolicitacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {

    @Query("SELECT COUNT(s) > 0 FROM Solicitacao s WHERE s.colaborador.id = :id AND s.status IN :status "
            + "AND s.dataInicio <= :fim AND s.dataFim >= :inicio")
    boolean existeSobreposicao(@Param("id") Long idColaborador,
                               @Param("status") Collection<StatusSolicitation> status,
                               @Param("inicio") LocalDate inicio,
                               @Param("fim") LocalDate fim);

    @Query("SELECT s FROM Solicitacao s JOIN FETCH s.colaborador LEFT JOIN FETCH s.aprovador ORDER BY s.id DESC")
    List<Solicitacao> findAllOtimizado();

    @Query(value = "SELECT s FROM Solicitacao s JOIN FETCH s.colaborador LEFT JOIN FETCH s.aprovador "
            + "WHERE s.status IN :status AND s.tipo IN :tipos ORDER BY s.id DESC",
            countQuery = "SELECT COUNT(s) FROM Solicitacao s WHERE s.status IN :status AND s.tipo IN :tipos")
    Page<Solicitacao> buscarPorFiltro(@Param("status") Collection<StatusSolicitation> status,
                                      @Param("tipos") Collection<TipoSolicitacao> tipos,
                                      Pageable pageable);

    @Query("SELECT s FROM Solicitacao s JOIN FETCH s.colaborador LEFT JOIN FETCH s.aprovador "
            + "WHERE s.status IN :status AND s.tipo IN :tipos ORDER BY s.id DESC")
    List<Solicitacao> listarPorFiltro(@Param("status") Collection<StatusSolicitation> status,
                                      @Param("tipos") Collection<TipoSolicitacao> tipos);

    @Query("SELECT s FROM Solicitacao s JOIN FETCH s.colaborador LEFT JOIN FETCH s.aprovador WHERE s.colaborador.id = :id ORDER BY s.id DESC")
    List<Solicitacao> findByColaboradorIdOtimizado(@Param("id") Long id);

    long countByStatus(StatusSolicitation status);

    long countByStatusAndAtualizadoEmAfter(StatusSolicitation status, LocalDateTime data);

    @Query("SELECT COUNT(s) FROM Solicitacao s WHERE s.status IN :statusList AND :hoje BETWEEN s.dataInicio AND s.dataFim")
    long countFolgasAtivasHoje(@Param("statusList") List<StatusSolicitation> statusList, @Param("hoje") LocalDate hoje);

    @Query("SELECT COUNT(s) FROM Solicitacao s WHERE s.status = :status AND s.dataInicio > :hoje AND s.dataInicio <= :daquiA7Dias")
    long countFolgasProximosDias(@Param("status") StatusSolicitation status, @Param("hoje") LocalDate hoje, @Param("daquiA7Dias") LocalDate daquiA7Dias);
}