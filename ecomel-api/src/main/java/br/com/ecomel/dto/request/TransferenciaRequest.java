package br.com.ecomel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TransferenciaRequest(
    @NotNull Long usuarioOrigemId,
    @NotBlank String codigoDestino, // Ex: AAA111
    @NotNull @Positive BigDecimal valorReal, // Valor valorizado que deseja enviar
    @NotBlank String requestKey // UUID único gerado no clique do botão
) {}