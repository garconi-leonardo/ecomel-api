CREATE TABLE ordens_favos (
    id BIGSERIAL PRIMARY KEY,
    carteira_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    quantidade_original DECIMAL(20, 8) NOT NULL,
    quantidade_restante DECIMAL(20, 8) NOT NULL,
    preco_unitario DECIMAL(20, 18) NOT NULL,
    status VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL,
    criado_por VARCHAR(255),
    atualizado_em TIMESTAMP,
    atualizado_por VARCHAR(255),
    desativado_em TIMESTAMP,
    desativado_por VARCHAR(255),
    CONSTRAINT fk_ordem_carteira FOREIGN KEY (carteira_id) REFERENCES carteiras(id)
);

CREATE INDEX idx_ordem_status_tipo ON ordens_favos(status, tipo, preco_unitario);
