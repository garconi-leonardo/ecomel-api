-- 1. Alterar a tabela de usuários para permitir e-mail nulo
ALTER TABLE usuarios ALTER COLUMN email DROP NOT NULL;

-- 2. Adicionar a coluna codigo_endereco na tabela de carteiras
-- Permitimos NULO inicialmente para não quebrar os registros que já existem
ALTER TABLE carteiras ADD COLUMN codigo_endereco VARCHAR(20);

-- 3. Criar um índice para que a busca de "existsByCodigoEndereco" seja ultra rápida
CREATE INDEX idx_carteira_codigo_busca ON carteiras(codigo_endereco);

-- 4. (Opcional) Script para preencher carteiras antigas que estão nulas
-- Isso evita erro de Unique Constraint se você tentar ativar a trava com campos vazios
-- Aqui usamos um gerador simples baseado no ID para os legados:
UPDATE carteiras 
SET codigo_endereco = 'LEG' || LPAD(id::text, 3, '0') 
WHERE codigo_endereco IS NULL;

-- 5. Agora que todos têm código, aplicamos a restrição de UNICIDADE (Constraint)
-- Isso garante no nível do banco que AAA001 nunca será duplicado
ALTER TABLE carteiras ALTER COLUMN codigo_endereco SET NOT NULL;
ALTER TABLE carteiras ADD CONSTRAINT uk_carteira_codigo_endereco UNIQUE (codigo_endereco);
