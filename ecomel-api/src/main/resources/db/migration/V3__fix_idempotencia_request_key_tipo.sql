-- =====================================================================
-- V3: Idempotência por (request_key + tipo)
-- ---------------------------------------------------------------------
-- Motivo: O UNIQUE simples em request_key impedia operações sequenciais
-- de tipos diferentes (ex.: DEPOSITO seguido de SAQUE) quando o front
-- reutiliza a chave (como o código da carteira). Agora a duplicidade
-- só é bloqueada para a MESMA combinação de chave + tipo de operação.
-- =====================================================================

-- 1. Remove o UNIQUE constraint antigo (gerado automaticamente pelo Hibernate
--    a partir do unique = true). Em PostgreSQL o nome padrão é
--    transacoes_request_key_key. Usamos IF EXISTS para tolerância.
ALTER TABLE transacoes DROP CONSTRAINT IF EXISTS transacoes_request_key_key;

-- 2. Remove eventual índice unique solto de migrações anteriores
DROP INDEX IF EXISTS idx_transacao_request_key;

-- 3. Cria o UNIQUE COMPOSTO (request_key, tipo).
--    "WHERE request_key IS NOT NULL" garante que transações sem chave
--    (caso surjam) não conflitem entre si.
CREATE UNIQUE INDEX uk_transacao_request_key_tipo
    ON transacoes (request_key, tipo)
    WHERE request_key IS NOT NULL;

-- 4. Recria índice de busca simples (não-único) para performance do extrato
CREATE INDEX idx_transacao_request_key ON transacoes(request_key);
