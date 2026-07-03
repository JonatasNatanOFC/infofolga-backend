package com.infoway.infofolga.repository;

import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {

    long countByStatus(StatusSolicitation status);

    @Query("SELECT COUNT(s) FROM Solicitacao s WHERE s.status = :status AND s.atualizadoEm >= :desde")
    long countByStatusAndAtualizadoEmAfter(
            @Param("status") StatusSolicitation status,
            @Param("desde") LocalDateTime desde);

    List<Solicitacao> findByFuncionarioId(Long funcionarioId);

    List<Solicitacao> findBySolicitanteGerenteId(Long gerenteId);

    List<Solicitacao> findByGerenteId(Long gerenteId);

    List<Solicitacao> findByFuncionarioIdOrderByCriadoEmDesc(Long funcionarioId);

    List<Solicitacao> findBySolicitanteGerenteIdOrderByCriadoEmDesc(Long gerenteId);

    List<Solicitacao> findByStatusOrderByCriadoEmDesc(StatusSolicitation status);

    long countByFuncionarioIdAndStatus(Long funcionarioId, StatusSolicitation status);

    @Query("SELECT s FROM Solicitacao s WHERE s.funcionario.id = :funcionarioId AND s.status = com.infoway.infofolga.model.StatusSolicitation.APROVADA")
    List<Solicitacao> findApproved(@Param("funcionarioId") Long funcionarioId);
}