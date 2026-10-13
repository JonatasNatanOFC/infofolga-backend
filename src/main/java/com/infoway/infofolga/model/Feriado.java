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

    /** Repete todo ano no mesmo dia e mês, a partir do ano de {@link #data}. */
    @Column(nullable = false)
    private boolean anual;

    /** A ocorrência deste feriado no ano pedido, ou vazio se não cai nele. */
    public java.util.Optional<LocalDate> noAno(int ano) {
        if (!anual) {
            return data.getYear() == ano ? java.util.Optional.of(data) : java.util.Optional.empty();
        }
        return ano < data.getYear() ? java.util.Optional.empty() : java.util.Optional.of(data.withYear(ano));
    }
}
