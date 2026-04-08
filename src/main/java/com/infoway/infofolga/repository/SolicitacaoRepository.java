package com.infoway.infofolga.repository;

import com.infoway.infofolga.model.Solicitacao;
import com.infoway.infofolga.model.StatusSolicitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {
    long countByStatus(StatusSolicitation status);

    @Query("SELECT COUNT(s) FROM Solicitacao s WHERE s.status = :status AND s.atualizadoEm >= :desde")
    long countByStatusAndAtualizadoEmAfter(StatusSolicitation status, LocalDateTime desde);
}
