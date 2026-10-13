package com.infoway.infofolga.model;

import com.infoway.infofolga.pontuacao.PoliticaSemSaldo;
import com.infoway.infofolga.pontuacao.RegrasPontuacao;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * Regras de pontuação de uma empresa a partir de uma data. Nunca é editada: cada alteração cria uma linha nova,
 * para que mudar um peso hoje não mude os pontos de dias passados.
 */
@Table(name = "configuracao_pontuacao")
@Entity(name = "ConfiguracaoPontuacao")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class ConfiguracaoPontuacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    // Jornada prevista em minutos.
    @Column(name = "jornada_seg", nullable = false) private int jornadaSeg;
    @Column(name = "jornada_ter", nullable = false) private int jornadaTer;
    @Column(name = "jornada_qua", nullable = false) private int jornadaQua;
    @Column(name = "jornada_qui", nullable = false) private int jornadaQui;
    @Column(name = "jornada_sex", nullable = false) private int jornadaSex;
    @Column(name = "jornada_sab", nullable = false) private int jornadaSab;
    @Column(name = "jornada_dom", nullable = false) private int jornadaDom;

    @Column(name = "tolerancia_min", nullable = false)
    private int toleranciaMin = 10;

    @Column(name = "peso_domingo", nullable = false, precision = 4, scale = 2)
    private BigDecimal pesoDomingo = BigDecimal.ONE;

    @Column(name = "peso_feriado", nullable = false, precision = 4, scale = 2)
    private BigDecimal pesoFeriado = BigDecimal.ONE;

    @Column(name = "fator_hora_noturna", nullable = false, precision = 14, scale = 12)
    private BigDecimal fatorHoraNoturna = RegrasPontuacao.HORA_NOTURNA_CLT;

    @Column(name = "compensar_falta", nullable = false)
    private boolean compensarFalta;

    @Enumerated(EnumType.STRING)
    @Column(name = "politica_sem_saldo", nullable = false)
    private PoliticaSemSaldo politicaSemSaldo = PoliticaSemSaldo.AVISAR;

    /** Prazo para usar os pontos; null = sem prazo definido. */
    @Column(name = "validade_meses")
    private Integer validadeMeses;

    /** Quem criou esta versão das regras (CEO ou RH). */
    @Column(name = "alterado_por")
    private Long alteradoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        criadoEm = LocalDateTime.now();
    }

    public RegrasPontuacao paraRegras() {
        Map<DayOfWeek, Duration> jornada = new EnumMap<>(DayOfWeek.class);
        jornada.put(DayOfWeek.MONDAY, Duration.ofMinutes(jornadaSeg));
        jornada.put(DayOfWeek.TUESDAY, Duration.ofMinutes(jornadaTer));
        jornada.put(DayOfWeek.WEDNESDAY, Duration.ofMinutes(jornadaQua));
        jornada.put(DayOfWeek.THURSDAY, Duration.ofMinutes(jornadaQui));
        jornada.put(DayOfWeek.FRIDAY, Duration.ofMinutes(jornadaSex));
        jornada.put(DayOfWeek.SATURDAY, Duration.ofMinutes(jornadaSab));
        jornada.put(DayOfWeek.SUNDAY, Duration.ofMinutes(jornadaDom));
        return new RegrasPontuacao(jornada, Duration.ofMinutes(toleranciaMin), pesoDomingo, pesoFeriado,
                fatorHoraNoturna, compensarFalta);
    }
}
