package com.infoway.infofolga.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Lançamento manual no banco de horas (saldo trazido de outro sistema, correção), com quem lançou e por quê. */
@Table(name = "ajustes_saldo")
@Entity(name = "AjusteSaldo")
@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class AjusteSaldo {

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

    @Column(nullable = false, updatable = false, precision = 7, scale = 2)
    private BigDecimal pontos;

    @Column(nullable = false, updatable = false)
    private String justificativa;

    @Column(name = "lancado_por", nullable = false, updatable = false)
    private Long lancadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public AjusteSaldo(Long colaboradorId, LocalDate data, BigDecimal pontos, String justificativa, Long lancadoPor) {
        this.colaboradorId = colaboradorId;
        this.data = data;
        this.pontos = pontos;
        this.justificativa = justificativa;
        this.lancadoPor = lancadoPor;
        this.criadoEm = LocalDateTime.now();
    }
}
