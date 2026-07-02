package com.infoway.infofolga.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
public class Solicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne
    @JoinColumn(name = "funcionario_id")
    @JsonIgnoreProperties({"senha", "solicitacoes", "foto"})
    private Funcionario funcionario;

    @Setter
    @ManyToOne
    @JoinColumn(name = "gerente_id")
    @JsonIgnoreProperties({"senha", "solicitacoes", "foto"})
    private Gerente gerente;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSolicitacao tipo;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusSolicitation status;

    @Setter
    @Column(nullable = false)
    private LocalDate dataInicio;

    @Setter
    @Column(nullable = false)
    private LocalDate dataFim;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String motivoResposta;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    private LocalDateTime atualizadoEm;

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