-- 1. Tabela de Usuários (E-mail opcional conforme nova regra)
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

-- 2. Tabela de Perfis/Roles
CREATE TABLE usuario_perfis (
    usuario_id BIGINT NOT NULL,
    perfil VARCHAR(100) NOT NULL,
    CONSTRAINT fk_usuario_perfis FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

-- 3. Tabela de Carteiras (Inclui Código Alfanumérico e controle de dividendos)
CREATE TABLE carteiras (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE,
    codigo_endereco VARCHAR(20) NOT NULL UNIQUE, 
    saldo_base DECIMAL(20, 18) NOT NULL DEFAULT 0,
    saldo_favos DECIMAL(20, 8) NOT NULL DEFAULT 0,
    ultimo_indice_favo DECIMAL(20, 18) NOT NULL DEFAULT 0, 
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL,
    criado_por VARCHAR(255),
    atualizado_em TIMESTAMP,
    atualizado_por VARCHAR(255),
    desativado_em TIMESTAMP,
    desativado_por VARCHAR(255),
    CONSTRAINT fk_carteira_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_carteira_codigo_busca ON carteiras(codigo_endereco);

-- 4. Tabela de Transações (Com chave de Idempotência request_key)
CREATE TABLE transacoes (
    id BIGSERIAL PRIMARY KEY,
    carteira_id BIGINT NOT NULL,
    carteira_destino_id BIGINT, 
    request_key VARCHAR(64) UNIQUE, -- Proteção contra transações duplicadas
    tipo VARCHAR(50) NOT NULL,
    valor_bruto DECIMAL(20, 18) NOT NULL,
    valor_liquido DECIMAL(20, 18) NOT NULL,
    taxa_total DECIMAL(20, 18) NOT NULL,
    status VARCHAR(50) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL,
    criado_por VARCHAR(255),
    atualizado_em TIMESTAMP,
    atualizado_por VARCHAR(255),
    desativado_em TIMESTAMP,
    desativado_por VARCHAR(255),
    CONSTRAINT fk_transacao_carteira FOREIGN KEY (carteira_id) REFERENCES carteiras(id),
    CONSTRAINT fk_transacao_destino FOREIGN KEY (carteira_destino_id) REFERENCES carteiras(id)
);

CREATE INDEX idx_transacao_carteira_id ON transacoes(carteira_id);
CREATE INDEX idx_transacao_request_key ON transacoes(request_key);

-- 5. Tabela de Índice Global (Com acumulador de dividendos de FAVOS)
CREATE TABLE indice_global (
    id BIGSERIAL PRIMARY KEY,
    valor DECIMAL(20, 18) NOT NULL DEFAULT 1.0,
    indice_favo_acumulado DECIMAL(20, 18) NOT NULL DEFAULT 0, 
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL,
    criado_por VARCHAR(255),
    atualizado_em TIMESTAMP,
    atualizado_por VARCHAR(255),
    desativado_em TIMESTAMP,
    desativado_por VARCHAR(255)
);

-- Inserir o índice inicial obrigatório
INSERT INTO indice_global (valor, indice_favo_acumulado, ativo, criado_em, criado_por) 
VALUES (1.000000000000000000, 0.000000000000000000, TRUE, CURRENT_TIMESTAMP, 'SYSTEM');
