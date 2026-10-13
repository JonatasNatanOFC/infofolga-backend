package com.infoway.infofolga.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Cliente do InfoFolga. Não tem @TenantId: é a própria empresa, e não um dado dela.
 */
@Table(name = "empresas")
@Entity(name = "Empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    /** Data URI da logo para fundo escuro (cabeçalho do app). */
    @Column(columnDefinition = "TEXT")
    private String logo;

    /** Data URI da logo para fundo claro (relatórios em PDF). */
    @Column(name = "logo_relatorio", columnDefinition = "TEXT")
    private String logoRelatorio;
}
