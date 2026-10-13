-- Retrato do banco de horas quando a folga foi pedida, para o gerente decidir sabendo se havia saldo.
ALTER TABLE solicitacoes ADD COLUMN saldo_no_pedido NUMERIC(7,2);
ALTER TABLE solicitacoes ADD COLUMN pontos_da_folga NUMERIC(7,2);
