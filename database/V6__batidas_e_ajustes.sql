-- Batidas brutas do ponto: o InfoFolga recalcula qualquer período sem depender do sistema de ponto de origem.
CREATE TABLE batidas (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    colaborador_id BIGINT NOT NULL REFERENCES colaboradores(id),
    data_hora TIMESTAMP NOT NULL,
    -- De onde veio: AFD (arquivo do relógio) ou, no futuro, a API do fornecedor.
    origem VARCHAR(20) NOT NULL,
    importado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    -- Importar o mesmo arquivo de novo não duplica nada.
    CONSTRAINT uk_batidas UNIQUE (colaborador_id, data_hora)
);

-- Lançamentos manuais no banco de horas: saldo trazido de outro sistema, correções.
CREATE TABLE ajustes_saldo (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    colaborador_id BIGINT NOT NULL REFERENCES colaboradores(id),
    data DATE NOT NULL,
    pontos NUMERIC(7,2) NOT NULL,
    justificativa VARCHAR(255) NOT NULL,
    lancado_por BIGINT NOT NULL REFERENCES colaboradores(id),
    criado_em TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ajustes_saldo_colaborador ON ajustes_saldo(colaborador_id, data);
