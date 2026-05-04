-- =========================================
-- 1. USUÁRIOS
-- =========================================
CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    senha VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL,
    criado_por VARCHAR(255),
    atualizado_em TIMESTAMP,
    atualizado_por VARCHAR(255),
    desativado_em TIMESTAMP,
    desativado_por VARCHAR(255)
);

-- =========================================
-- 2. PERFIS
-- =========================================
CREATE TABLE usuario_perfis (
    usuario_id BIGINT NOT NULL,
    perfil VARCHAR(100) NOT NULL,
    CONSTRAINT fk_usuario_perfis 
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

-- =========================================
-- 3. CARTEIRAS (COM BLOQUEIO E CONCORRÊNCIA)
-- =========================================
CREATE TABLE carteiras (
    versao BIGINT NOT NULL DEFAULT 0,
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE,
    codigo_carteira VARCHAR(20) NOT NULL UNIQUE,

    -- ECOMEL
    token_ecomel DECIMAL(38, 8) NOT NULL DEFAULT 0,
    token_ecomel_bloqueado DECIMAL(38, 8) NOT NULL DEFAULT 0,

    -- FAVOS
    saldo_favos DECIMAL(38, 8) NOT NULL DEFAULT 0,
    saldo_favos_bloqueado DECIMAL(38, 8) NOT NULL DEFAULT 0,

    -- Dividendos
    ultimo_indice_favo DECIMAL(38, 18) NOT NULL DEFAULT 0,

    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    criado_em TIMESTAMP NOT NULL,
    criado_por VARCHAR(255),
    atualizado_em TIMESTAMP,
    atualizado_por VARCHAR(255),
    desativado_em TIMESTAMP,
    desativado_por VARCHAR(255),

    CONSTRAINT fk_carteira_usuario 
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_carteira_codigo_busca 
ON carteiras(codigo_carteira);

CREATE INDEX idx_carteira_saldo 
ON carteiras (saldo_favos, saldo_favos_bloqueado);

-- =========================================
-- 4. TRANSAÇÕES (IDEMPOTÊNCIA)
-- =========================================
CREATE TABLE transacoes (
    id BIGSERIAL PRIMARY KEY,
    carteira_id BIGINT NOT NULL,
    carteira_destino_id BIGINT,
    request_key VARCHAR(64) UNIQUE,

    tipo VARCHAR(50) NOT NULL,
    valor_bruto DECIMAL(38, 18) NOT NULL,
    valor_liquido DECIMAL(38, 18) NOT NULL,
    taxa_total DECIMAL(38, 18) NOT NULL,

    status VARCHAR(50) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    criado_em TIMESTAMP NOT NULL,
    criado_por VARCHAR(255),
    atualizado_em TIMESTAMP,
    atualizado_por VARCHAR(255),
    desativado_em TIMESTAMP,
    desativado_por VARCHAR(255),

    CONSTRAINT fk_transacao_carteira 
        FOREIGN KEY (carteira_id) REFERENCES carteiras(id),

    CONSTRAINT fk_transacao_destino 
        FOREIGN KEY (carteira_destino_id) REFERENCES carteiras(id)
);

CREATE INDEX idx_transacao_carteira_id 
ON transacoes(carteira_id);

CREATE INDEX idx_transacao_request_key 
ON transacoes(request_key);

-- =========================================
-- 5. INDICE GLOBAL (ECOMEL + FAVOS)
-- =========================================
CREATE TABLE indice_global (
    versao BIGINT NOT NULL DEFAULT 0,
    id BIGSERIAL PRIMARY KEY,

    valor DECIMAL(38, 18) NOT NULL DEFAULT 1.0,
    liquidez_total DECIMAL(38, 18) NOT NULL DEFAULT 0,
    indice_favo_acumulado DECIMAL(38, 18) NOT NULL DEFAULT 0,

    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    criado_em TIMESTAMP NOT NULL,
    criado_por VARCHAR(255),
    atualizado_em TIMESTAMP,
    atualizado_por VARCHAR(255),
    desativado_em TIMESTAMP,
    desativado_por VARCHAR(255)
);

-- Índice inicial
INSERT INTO indice_global (
    valor, liquidez_total, indice_favo_acumulado, ativo, criado_em, criado_por
) VALUES (
    1.000000000000000000,
    0.000000000000000000,
    0.000000000000000000,
    TRUE,
    CURRENT_TIMESTAMP,
    'ECOMEL'
);

-- =========================================
-- 6. ORDENS DE FAVOS (ORDER BOOK)
-- =========================================
CREATE TABLE ordens_favos (
    id BIGSERIAL PRIMARY KEY,
    versao BIGINT DEFAULT 0 NOT NULL,
    carteira_id BIGINT NOT NULL,
    request_key VARCHAR(64) UNIQUE,

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

    CONSTRAINT fk_ordem_carteira 
        FOREIGN KEY (carteira_id) REFERENCES carteiras(id),

    CONSTRAINT chk_tipo 
        CHECK (tipo IN ('COMPRA', 'VENDA')),

    CONSTRAINT chk_status 
        CHECK (status IN ('ABERTA', 'PARCIALMENTE_EXECUTADA', 'EXECUTADA', 'CANCELADA'))
);
CREATE INDEX idx_ordens_status_tipo 
ON ordens_favos (status, tipo);

CREATE INDEX idx_ordens_preco 
ON ordens_favos (preco_unitario);

-- Índice do book (preço + prioridade temporal)
CREATE INDEX idx_ordem_book 
ON ordens_favos(status, tipo, preco_unitario, criado_em);

CREATE INDEX idx_ordens_request_key
ON ordens_favos(request_key);
-- =========================================
-- 7. EXECUÇÕES (HISTÓRICO DE NEGÓCIOS)
-- =========================================
CREATE TABLE ordens_execucoes (
    id BIGSERIAL PRIMARY KEY,

    ordem_compra_id BIGINT NOT NULL,
    ordem_venda_id BIGINT NOT NULL,

    quantidade DECIMAL(20,8) NOT NULL,
    preco_execucao DECIMAL(20,18) NOT NULL,

    criado_em TIMESTAMP NOT NULL,

    CONSTRAINT fk_exec_compra 
        FOREIGN KEY (ordem_compra_id) REFERENCES ordens_favos(id),

    CONSTRAINT fk_exec_venda 
        FOREIGN KEY (ordem_venda_id) REFERENCES ordens_favos(id)
);

CREATE INDEX idx_exec_compra 
ON ordens_execucoes(ordem_compra_id);

CREATE INDEX idx_exec_venda 
ON ordens_execucoes(ordem_venda_id);