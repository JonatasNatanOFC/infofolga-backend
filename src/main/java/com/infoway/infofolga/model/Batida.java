package com.infoway.infofolga.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.time.LocalDateTime;

/** Uma batida de ponto como veio da fonte. Nunca é alterada: correções entram como novas batidas no relógio. */
@Table(name = "batidas")
@Entity(name = "Batida")
@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Batida {

    /** De onde a batida veio. A API do fornecedor entra aqui quando for integrada. */
    public enum Origem { AFD }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId;

    @Column(name = "colaborador_id", nullable = false, updatable = false)
    private Long colaboradorId;

    @Column(name = "data_hora", nullable = false, updatable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Origem origem;

    @Column(name = "importado_em", nullable = false, updatable = false)
    private LocalDateTime importadoEm;

    public Batida(Long colaboradorId, LocalDateTime dataHora, Origem origem) {
        this.colaboradorId = colaboradorId;
        this.dataHora = dataHora;
        this.origem = origem;
        this.importadoEm = LocalDateTime.now();
    }
}
