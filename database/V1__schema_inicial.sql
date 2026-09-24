-- V1__schema_inicial.sql
-- Schema inicial do InfoFolga — reproduz exatamente o mapeamento JPA
-- Executar em um banco PostgreSQL vazio antes de subir a API com SPRING_JPA_HIBERNATE_DDL_AUTO=validate

-- Tabela colaboradores
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

-- Tabela solicitacoes
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

-- Índices em Foreign Keys para performance
CREATE INDEX idx_solicitacoes_colaborador_id ON solicitacoes(colaborador_id);
CREATE INDEX idx_solicitacoes_aprovador_id ON solicitacoes(aprovador_id);

-- Índices em colunas frequentemente filtradas (queries do repository)
CREATE INDEX idx_solicitacoes_status ON solicitacoes(status);
CREATE INDEX idx_solicitacoes_tipo_status ON solicitacoes(tipo, status);
CREATE INDEX idx_solicitacoes_data_inicio_data_fim ON solicitacoes(data_inicio, data_fim);

-- Índice em atualizado_em para countByStatusAndAtualizadoEmAfter
CREATE INDEX idx_solicitacoes_atualizado_em ON solicitacoes(atualizado_em);
