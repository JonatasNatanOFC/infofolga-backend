package com.infoway.infofolga.repository;

import com.infoway.infofolga.model.ConfiguracaoPontuacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConfiguracaoPontuacaoRepository extends JpaRepository<ConfiguracaoPontuacao, Long> {

    /** Regras que valiam em uma data: a versão mais recente que já tinha começado. */
    Optional<ConfiguracaoPontuacao> findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(LocalDate data);

    Optional<ConfiguracaoPontuacao> findByVigenteDesde(LocalDate vigenteDesde);

    List<ConfiguracaoPontuacao> findAllByOrderByVigenteDesdeDesc();
}
