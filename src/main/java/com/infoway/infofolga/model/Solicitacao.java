package com.infoway.infofolga.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Table(name = "solicitacoes")
@Entity(name = "Solicitacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Solicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Preenchido e filtrado pelo Hibernate a partir da empresa do usuário logado (EmpresaAtualResolver).
    @TenantId
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId;

    @ManyToOne
    @JoinColumn(name = "colaborador_id")
    private Colaborador colaborador;

    @ManyToOne
    @JoinColumn(name = "aprovador_id")
    private Colaborador aprovador;

    @Column(name = "nome_historico")
    private String nomeHistorico;

    @Column(name = "cargo_historico")
    private String cargoHistorico;

    @Column(name = "setor_historico")
    private String setorHistorico;

    @Column(name = "foto_historico", columnDefinition = "TEXT")
    private String fotoHistorico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSolicitacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusSolicitation status;

    @Column(nullable = false)
    private LocalDate dataInicio;

    @Column(nullable = false)
    private LocalDate dataFim;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String motivoResposta;

    // Só para folgas. Pedidos antigos (antes da pontuação) e férias ficam sem motivo e não mexem no saldo.
    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_folga")
    private MotivoFolga motivoFolga;

    @Column(name = "desconta_saldo")
    private Boolean descontaSaldo;

    /** Saldo disponível quando a folga do banco foi pedida (null quando não se aplica). */
    @Column(name = "saldo_no_pedido", precision = 7, scale = 2)
    private java.math.BigDecimal saldoNoPedido;

    /** Quantos pontos a folga usa: a jornada prevista dos dias pedidos. */
    @Column(name = "pontos_da_folga", precision = 7, scale = 2)
    private java.math.BigDecimal pontosDaFolga;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    private LocalDateTime atualizadoEm;

    // Lock otimista: impede que duas avaliações simultâneas sobrescrevam uma à outra.
    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long versao;

    @PrePersist
    public void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }
}