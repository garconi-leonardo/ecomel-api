package br.com.ecomel.dto.request;

import br.com.ecomel.domain.enums.TipoOrdem;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record OrdemFavoRequest(
    @NotNull Long usuarioId,
    @NotNull TipoOrdem tipo,
    @NotNull @Positive BigDecimal quantidade,
    @NotNull @Positive BigDecimal precoUnitario
) {}