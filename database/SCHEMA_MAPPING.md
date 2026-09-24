# Mapeamento JPA → SQL

Documento técnico: campo por campo, cada entidade JPA mapeada para SQL.

## Entidade: Colaborador

### Java code:
```java
@Table(name = "colaboradores")
@Entity(name = "Colaborador")
public class Colaborador implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nome;
    
    @Column(unique = true)
    private String cpf;
    
    @Column(unique = true)
    private String email;
    
    private String senha;
    
    private String cargo;
    private String setor;
    
    @Column(columnDefinition = "TEXT")
    private String foto;
    
    @Enumerated(EnumType.STRING)
    private Role role;
    
    @OneToMany(mappedBy = "colaborador")
    private List<Solicitacao> minhasSolicitacoes; // só do lado Java
    
    private String status;
}
```

### Mapeamento SQL:

| Campo Java | Anotação | SQL | Tipo | Nullable | Unique | Default |
|---|---|---|---|---|---|---|
| id | @Id @GeneratedValue(IDENTITY) | id | BIGSERIAL | NO | YES (PK) | auto-increment |
| nome | — | nome | VARCHAR(255) | YES | NO | — |
| cpf | @Column(unique=true) | cpf | VARCHAR(255) | YES | YES | — |
| email | @Column(unique=true) | email | VARCHAR(255) | YES | YES | — |
| senha | — | senha | VARCHAR(255) | YES | NO | — |
| cargo | — | cargo | VARCHAR(255) | YES | NO | — |
| setor | — | setor | VARCHAR(255) | YES | NO | — |
| foto | @Column(columnDefinition="TEXT") | foto | TEXT | YES | NO | — |
| role | @Enumerated(STRING) | role | VARCHAR(255) | YES | NO | — |
| minhasSolicitacoes | @OneToMany(mappedBy="colaborador") | — | — | — | — | — |
| status | — | status | VARCHAR(255) | YES | NO | — |

### SQL:
```sql
CREATE TABLE colaboradores (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255),
    cpf VARCHAR(255) UNIQUE,
    email VARCHAR(255) UNIQUE,
    senha VARCHAR(255),
    cargo VARCHAR(255),
    setor VARCHAR(255),
    foto TEXT,
    role VARCHAR(255),
    status VARCHAR(255)
);
```

---

## Entidade: Solicitacao

### Java code:
```java
@Table(name = "solicitacoes")
@Entity(name = "Solicitacao")
public class Solicitacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
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
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;
    
    private LocalDateTime atualizadoEm;
    
    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long versao;
    
    @PrePersist
    public void prePersist() { ... }
    
    @PreUpdate
    public void preUpdate() { ... }
}
```

### Mapeamento SQL:

| Campo Java | Anotação | SQL | Tipo | Nullable | FK | Default |
|---|---|---|---|---|---|---|
| id | @Id @GeneratedValue(IDENTITY) | id | BIGSERIAL | NO | — | auto-increment |
| colaborador | @ManyToOne @JoinColumn("colaborador_id") | colaborador_id | BIGINT | YES | colaboradores.id | — |
| aprovador | @ManyToOne @JoinColumn("aprovador_id") | aprovador_id | BIGINT | YES | colaboradores.id | — |
| nomeHistorico | @Column(name="nome_historico") | nome_historico | VARCHAR(255) | YES | — | — |
| cargoHistorico | @Column(name="cargo_historico") | cargo_historico | VARCHAR(255) | YES | — | — |
| setorHistorico | @Column(name="setor_historico") | setor_historico | VARCHAR(255) | YES | — | — |
| fotoHistorico | @Column(columnDefinition="TEXT") | foto_historico | TEXT | YES | — | — |
| tipo | @Enumerated(STRING) @Column(nullable=false) | tipo | VARCHAR(255) | NO | — | — |
| status | @Enumerated(STRING) @Column(nullable=false) | status | VARCHAR(255) | NO | — | — |
| dataInicio | @Column(nullable=false) | data_inicio | DATE | NO | — | — |
| dataFim | @Column(nullable=false) | data_fim | DATE | NO | — | — |
| motivo | @Column(columnDefinition="TEXT") | motivo | TEXT | YES | — | — |
| motivoResposta | @Column(columnDefinition="TEXT") | motivo_resposta | TEXT | YES | — | — |
| criadoEm | @Column(nullable=false, updatable=false) | criado_em | TIMESTAMP | NO | — | app (PrePersist) |
| atualizadoEm | — | atualizado_em | TIMESTAMP | YES | — | app (PrePersist/PreUpdate) |
| versao | @Version @Column(nullable=false, columnDefinition="bigint default 0") | versao | BIGINT | NO | — | 0 |

### SQL:
```sql
CREATE TABLE solicitacoes (
    id BIGSERIAL PRIMARY KEY,
    colaborador_id BIGINT REFERENCES colaboradores(id),
    aprovador_id BIGINT REFERENCES colaboradores(id),
    nome_historico VARCHAR(255),
    cargo_historico VARCHAR(255),
    setor_historico VARCHAR(255),
    foto_historico TEXT,
    tipo VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    motivo TEXT,
    motivo_resposta TEXT,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 0
);
```

---

## Enums (persistidos como VARCHAR)

### Role (em colaboradores.role)
```java
public enum Role {
    FUNCIONARIO, GERENTE, CEO
}
// Valores persitidos: "FUNCIONARIO", "GERENTE", "CEO"
```

### TipoSolicitacao (em solicitacoes.tipo)
```java
public enum TipoSolicitacao {
    FOLGA, FERIAS
}
// Valores persistidos: "FOLGA", "FERIAS"
```

### StatusSolicitation (em solicitacoes.status)
```java
public enum StatusSolicitation {
    PENDENTE, APROVADA, REJEITADA, CANCELADA, USUFRUIDA, INVALIDADA, ESTORNO_PENDENTE
}
// Valores persistidos: "PENDENTE", "APROVADA", "REJEITADA", "CANCELADA", "USUFRUIDA", "INVALIDADA", "ESTORNO_PENDENTE"
```

---

## Índices

| Nome | Tabela | Colunas | Propósito |
|---|---|---|---|
| colaboradores_pkey | colaboradores | id | PK (automático) |
| colaboradores_cpf_key | colaboradores | cpf | UNIQUE (automático) |
| colaboradores_email_key | colaboradores | email | UNIQUE (automático) |
| solicitacoes_pkey | solicitacoes | id | PK (automático) |
| idx_solicitacoes_colaborador_id | solicitacoes | colaborador_id | FK, findByColaboradorIdOtimizado |
| idx_solicitacoes_aprovador_id | solicitacoes | aprovador_id | FK, boas práticas |
| idx_solicitacoes_status | solicitacoes | status | countByStatus, filtros |
| idx_solicitacoes_tipo_status | solicitacoes | tipo, status | buscarPorFiltro |
| idx_solicitacoes_data_inicio_data_fim | solicitacoes | data_inicio, data_fim | existeSobreposicao |
| idx_solicitacoes_atualizado_em | solicitacoes | atualizado_em | countByStatusAndAtualizadoEmAfter |

---

## Observações

1. **Lock otimista:** `@Version` no Hibernate cria uma coluna `versao BIGINT NOT NULL DEFAULT 0`. A cada UPDATE bem-sucedido, o Hibernate incrementa automaticamente. Se duas transações tentarem atualizar o mesmo registro, a segunda falha com `OptimisticLockingFailureException`.

2. **Timestamps:** `criadoEm` e `atualizadoEm` são populados pela aplicação (Java `LocalDateTime.now()`), não por trigger SQL. O `@PrePersist` e `@PreUpdate` rodam no ORM, não no banco.

3. **Foreign Keys:** Sem especificação `ON DELETE/UPDATE`, o padrão PostgreSQL é `RESTRICT` (equivalente a `NO ACTION`). Não é possível deletar um `Colaborador` se houver `Solicitacao` referenciando.

4. **Validação de schema:** Com `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`, o Hibernate verifica na inicialização que a estrutura real do banco corresponde exatamente ao mapping JPA. Qualquer divergência (tipo de coluna, nullability, constraints) resulta em erro e a aplicação não sobe.

5. **Enums:** Persistidos como VARCHAR com o nome da constante Java. Não há CHECK constraint no banco — o Hibernate valida.

6. **TEXT para fotos:** `foto` em `colaboradores` e `foto_historico` em `solicitacoes` usam TEXT para armazenar data URIs (base64 JPEG/PNG/WebP). O tamanho é ilimitado em PostgreSQL (até 1 GB por linhas). Para segurança, a API limita a 2 MB no frontend, mas o banco não enfor.
