package com.infoway.infofolga.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.time.LocalDate;

/** Feriado ou folga coletiva da empresa (municipal, estadual, ponte). Os nacionais são calculados. */
@Table(name = "feriados")
@Entity(name = "Feriado")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Feriado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false, length = 120)
    private String descricao;
}
