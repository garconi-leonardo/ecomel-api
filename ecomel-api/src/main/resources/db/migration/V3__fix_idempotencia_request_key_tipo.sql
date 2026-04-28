-- =====================================================================
-- V3: Idempotência por (request_key + tipo + valor_bruto)
-- ---------------------------------------------------------------------
-- Motivo: O UNIQUE simples em request_key impedia operações sequenciais
-- legítimas (ex.: DEPOSITO seguido de SAQUE, ou dois saques com valores
-- diferentes) quando o front reutiliza a chave (como o código da carteira).
--
-- Nova regra anti múltiplos cliques:
--   - Mesma chave + mesmo tipo + MESMO valor   => duplicata  (bloqueia)
--   - Mesma chave + mesmo tipo + valor DIFERENTE => operação legítima (libera)
--   - Mesma chave + tipo DIFERENTE              => operação legítima (libera)
-- =====================================================================

-- 1. Remove o UNIQUE constraint antigo (gerado pelo Hibernate via unique = true)
ALTER TABLE transacoes DROP CONSTRAINT IF EXISTS transacoes_request_key_key;

-- 2. Remove eventual índice unique solto de migrações anteriores
DROP INDEX IF EXISTS idx_transacao_request_key;
DROP INDEX IF EXISTS uk_transacao_request_key_tipo;

-- 3. Cria o UNIQUE COMPOSTO (request_key, tipo, valor_bruto).
--    "WHERE request_key IS NOT NULL" garante que transações sem chave
--    (caso surjam) não conflitem entre si.
CREATE UNIQUE INDEX uk_transacao_request_key_tipo_valor
    ON transacoes (request_key, tipo, valor_bruto)
    WHERE request_key IS NOT NULL;

-- 4. Recria índice de busca simples (não-único) para performance do extrato
CREATE INDEX idx_transacao_request_key ON transacoes(request_key);
