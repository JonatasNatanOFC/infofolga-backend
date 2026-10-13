-- V3__pontuacao.sql
-- Estrutura da pontuação de horas (banco de horas em pontos) e motivo da folga.
-- Não cria configuração para nenhuma empresa: a pontuação só começa quando a empresa definir a data de início.
-- Rodar UMA vez, com a API parada, depois de um backup.

BEGIN;

-- Regras da pontuação. Cada alteração é uma nova linha que vale a partir de "vigente_desde";
-- o cálculo de um dia usa a linha mais recente com vigente_desde <= dia. Assim, mudar um peso não altera o passado.
CREATE TABLE configuracao_pontuacao (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    vigente_desde DATE NOT NULL,
    -- Jornada prevista em minutos por dia da semana.
    jornada_seg INTEGER NOT NULL,
    jornada_ter INTEGER NOT NULL,
    jornada_qua INTEGER NOT NULL,
    jornada_qui INTEGER NOT NULL,
    jornada_sex INTEGER NOT NULL,
    jornada_sab INTEGER NOT NULL,
    jornada_dom INTEGER NOT NULL,
    tolerancia_min INTEGER NOT NULL DEFAULT 10,
    peso_domingo NUMERIC(4,2) NOT NULL DEFAULT 1,
    peso_feriado NUMERIC(4,2) NOT NULL DEFAULT 1,
    -- 60 / 52,5 = 8 / 7 (hora noturna reduzida da CLT).
    fator_hora_noturna NUMERIC(14,12) NOT NULL DEFAULT 1.142857142857,
    compensar_falta BOOLEAN NOT NULL DEFAULT FALSE,
    -- PERMITIR, AVISAR ou BLOQUEAR folga de banco de horas sem saldo.
    politica_sem_saldo VARCHAR(20) NOT NULL DEFAULT 'AVISAR',
    -- Prazo para usar os pontos; NULL = sem prazo definido.
    validade_meses INTEGER,
    alterado_por BIGINT REFERENCES colaboradores(id),
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_configuracao_pontuacao UNIQUE (empresa_id, vigente_desde),
    CONSTRAINT ck_politica_sem_saldo CHECK (politica_sem_saldo IN ('PERMITIR', 'AVISAR', 'BLOQUEAR'))
);

-- Feriados e folgas coletivas da empresa (municipais, estaduais, pontes). Os nacionais são calculados pelo sistema.
CREATE TABLE feriados (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    data DATE NOT NULL,
    descricao VARCHAR(120) NOT NULL,
    CONSTRAINT uk_feriados UNIQUE (empresa_id, data)
);

-- Resultado do cálculo de cada colaborador em cada dia. Recalculado do zero a cada importação.
CREATE TABLE apuracao_dia (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    colaborador_id BIGINT NOT NULL REFERENCES colaboradores(id),
    data DATE NOT NULL,
    situacao VARCHAR(30) NOT NULL,
    segundos_trabalhados INTEGER NOT NULL,
    segundos_previstos INTEGER NOT NULL,
    pontos NUMERIC(7,2) NOT NULL,
    inconsistente BOOLEAN NOT NULL DEFAULT FALSE,
    descricao VARCHAR(255) NOT NULL,
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_apuracao_dia UNIQUE (colaborador_id, data)
);
CREATE INDEX idx_apuracao_dia_empresa_data ON apuracao_dia(empresa_id, data);

-- Motivo da folga e se ela desconta do saldo. As folgas antigas ficam sem motivo e não descontam nada.
ALTER TABLE solicitacoes ADD COLUMN motivo_folga VARCHAR(30);
ALTER TABLE solicitacoes ADD COLUMN desconta_saldo BOOLEAN;

COMMIT;
