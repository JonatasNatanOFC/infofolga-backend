-- Feriado municipal que se repete todo ano (ex.: emancipação da cidade).
-- A data guardada é a da primeira ocorrência: o feriado vale daquele ano em diante, sempre no mesmo dia e mês.
ALTER TABLE feriados ADD COLUMN anual BOOLEAN NOT NULL DEFAULT false;
