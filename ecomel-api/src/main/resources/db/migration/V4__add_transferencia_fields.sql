-- Adiciona coluna para identificar o destino em transferências internas
ALTER TABLE transacoes ADD COLUMN carteira_destino_id BIGINT;

-- Cria índice para acelerar a busca do extrato
CREATE INDEX idx_transacao_carteira_id ON transacoes(carteira_id);
CREATE INDEX idx_transacao_destino_id ON transacoes(carteira_destino_id);

-- Adiciona a constraint de chave estrangeira
ALTER TABLE transacoes 
ADD CONSTRAINT fk_transacao_carteira_destino 
FOREIGN KEY (carteira_destino_id) REFERENCES carteiras(id);
