package br.com.ecomel.dto.response;

import br.com.ecomel.domain.enums.StatusOrdem;
import br.com.ecomel.domain.enums.TipoOrdem;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrdemFavoResponse(
    Long id,
    String codigoEnderecoCarteira, // Identificador AAA111 de quem criou a ordem
    TipoOrdem tipo,
    BigDecimal quantidadeOriginal,
    BigDecimal quantidadeRestante,
    BigDecimal precoUnitario,      // Preço em ECM que o usuário determinou
    StatusOrdem status,
    LocalDateTime criadoEm
) {}
