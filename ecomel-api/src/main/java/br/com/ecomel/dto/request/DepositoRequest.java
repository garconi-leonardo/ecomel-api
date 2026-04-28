package br.com.ecomel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record DepositoRequest(
    @NotNull Long usuarioId,
    @NotNull @Positive BigDecimal valor,
    @NotBlank String requestKey // CODIGO DA CARTEIRA
) {}