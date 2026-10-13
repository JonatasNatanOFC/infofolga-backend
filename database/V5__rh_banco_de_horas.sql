-- Se o RH da empresa acumula pontos: não quando é terceirizado (Infoway), sim quando é funcionário.
ALTER TABLE configuracao_pontuacao ADD COLUMN rh_tem_banco_de_horas BOOLEAN NOT NULL DEFAULT false;
