use dbecomel

CREATE OR REPLACE PROCEDURE creditar_favos_carteira(
    p_codigo_endereco VARCHAR,
    p_quantidade DECIMAL(38, 8),
    p_descricao VARCHAR DEFAULT 'AJUSTE_MANUAL_FAVOS'
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_carteira_id BIGINT;
    v_indice_atual DECIMAL(38, 18);
BEGIN

    -- 🔒 1. LOCK NA CARTEIRA
    SELECT id
    INTO v_carteira_id
    FROM carteiras
    WHERE codigo_endereco = p_codigo_endereco
    FOR UPDATE;

    IF v_carteira_id IS NULL THEN
        RAISE EXCEPTION 'Carteira não encontrada';
    END IF;

    -- 🔒 2. LOCK NO ÍNDICE GLOBAL
    SELECT indice_favo_acumulado
    INTO v_indice_atual
    FROM indice_global
    WHERE ativo = TRUE
    FOR UPDATE;

    -- 💰 3. ATUALIZA SALDO DE FAVOS
    UPDATE carteiras
    SET saldo_favos = saldo_favos + p_quantidade,
        atualizado_em = CURRENT_TIMESTAMP,
        atualizado_por = 'SCRIPT_FAVOS'
    WHERE id = v_carteira_id;

    -- 🧠 4. SINCRONIZA ÍNDICE DE DIVIDENDO
    -- (evita ganhar dividendos retroativos indevidos)
    UPDATE carteiras
    SET ultimo_indice_favo = v_indice_atual
    WHERE id = v_carteira_id;

    -- 🧾 5. REGISTRA TRANSAÇÃO
    INSERT INTO transacoes (
        carteira_id,
        tipo,
        valor_bruto,
        valor_liquido,
        taxa_total,
        status,
        request_key,
        ativo,
        criado_em,
        criado_por
    )
    VALUES (
        v_carteira_id,
        'AJUSTE_FAVOS',
        p_quantidade,
        p_quantidade,
        0,
        'CONCLUIDA',
        CONCAT('FAVO_', EXTRACT(EPOCH FROM NOW())),
        TRUE,
        CURRENT_TIMESTAMP,
        p_descricao
    );

END;

--COMMIT
--ROLLBACK
