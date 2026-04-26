-- Tabela de Usuários
CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
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

-- Tabela de Perfis/Roles
CREATE TABLE usuario_perfis (
    usuario_id BIGINT NOT NULL,
    perfil VARCHAR(100) NOT NULL,
    CONSTRAINT fk_usuario_perfis FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

-- Tabela de Carteiras
CREATE TABLE carteiras (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE,
    saldo_base DECIMAL(20, 18) NOT NULL DEFAULT 0,
    saldo_favos DECIMAL(20, 8) NOT NULL DEFAULT 0,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL,
    criado_por VARCHAR(255),
    atualizado_em TIMESTAMP,
    atualizado_por VARCHAR(255),
    desativado_em TIMESTAMP,
    desativado_por VARCHAR(255),
    CONSTRAINT fk_carteira_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

-- Tabela de Índice Global (Apenas um registro ativo por vez)
CREATE TABLE indice_global (
    id BIGSERIAL PRIMARY KEY,
    valor DECIMAL(20, 18) NOT NULL DEFAULT 1.0,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL,
    criado_por VARCHAR(255),
    atualizado_em TIMESTAMP,
    atualizado_por VARCHAR(255),
    desativado_em TIMESTAMP,
    desativado_por VARCHAR(255)
);

-- Inserir o índice inicial obrigatório
INSERT INTO indice_global (valor, ativo, criado_em, criado_por) 
VALUES (1.000000000000000000, TRUE, CURRENT_TIMESTAMP, 'SYSTEM');
