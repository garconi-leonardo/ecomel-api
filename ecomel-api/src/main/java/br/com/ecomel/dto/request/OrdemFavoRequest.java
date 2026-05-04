package br.com.ecomel.dto.request;

import br.com.ecomel.domain.enums.TipoOrdem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrdemFavoRequest(

	@NotNull(message = "Id do usuário é obrigatório")
    Long usuarioId,

    @NotNull(message = "Tipo da ordem é obrigatório")
    TipoOrdem tipo,

    @NotNull(message = "Quantidade é obrigatória")
    @Positive(message = "Quantidade deve ser maior que zero")
    BigDecimal quantidade,

    @NotNull(message = "Preço é obrigatório")
    @Positive(message = "Preço deve ser maior que zero")
    BigDecimal precoUnitario,

    @NotBlank(message = "RequestKey é obrigatório")
    String requestKey // 🔥 idempotência (anti duplicação)
) {}
