package br.com.ecomel.dto.response;

import br.com.ecomel.domain.enums.TipoTransacao;
import br.com.ecomel.domain.enums.StatusTransacao;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransacaoResponse(
    Long id,
    TipoTransacao tipo,
    BigDecimal valorBruto,
    BigDecimal valorLiquido,
    BigDecimal taxaTotal,
    StatusTransacao status,
    LocalDateTime data,
    String codigoDestino // Preenchido apenas em transferências internas
) {}
