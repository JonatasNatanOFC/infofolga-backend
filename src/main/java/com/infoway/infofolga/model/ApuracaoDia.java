package com.infoway.infofolga.model;

import com.infoway.infofolga.pontuacao.ResultadoDia;
import com.infoway.infofolga.pontuacao.SituacaoDia;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Pontos de um colaborador em um dia. Recalculado do zero a cada importação do ponto. */
@Table(name = "apuracao_dia")
@Entity(name = "ApuracaoDia")
@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class ApuracaoDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId;

    @Column(name = "colaborador_id", nullable = false, updatable = false)
    private Long colaboradorId;

    @Column(nullable = false, updatable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoDia situacao;

    @Column(name = "segundos_trabalhados", nullable = false)
    private int segundosTrabalhados;

    @Column(name = "segundos_previstos", nullable = false)
    private int segundosPrevistos;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal pontos;

    @Column(nullable = false)
    private boolean inconsistente;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public ApuracaoDia(Long colaboradorId, ResultadoDia resultado) {
        this.colaboradorId = colaboradorId;
        this.data = resultado.data();
        atualizarCom(resultado);
    }

    /** Recalcular o mesmo dia substitui o resultado anterior: nunca soma duas vezes. */
    public void atualizarCom(ResultadoDia resultado) {
        if (!resultado.data().equals(data)) {
            throw new IllegalArgumentException("Resultado de outro dia: " + resultado.data());
        }
        situacao = resultado.situacao();
        segundosTrabalhados = Math.toIntExact(resultado.segundosTrabalhados());
        segundosPrevistos = Math.toIntExact(resultado.segundosPrevistos());
        pontos = resultado.pontos();
        inconsistente = resultado.inconsistente();
        descricao = resultado.descricao();
        atualizadoEm = LocalDateTime.now();
    }
}
