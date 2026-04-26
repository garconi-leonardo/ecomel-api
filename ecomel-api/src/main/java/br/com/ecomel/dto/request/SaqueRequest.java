package br.com.ecomel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * DTO para solicitação de saque.
 * O valor informado é o valor "Real" (valorizado) que o usuário deseja retirar.
 */
public record SaqueRequest(
    @NotNull(message = "O ID do usuário é obrigatório") 
    Long usuarioId,

    @NotNull(message = "O valor do saque é obrigatório")
    @Positive(message = "O valor deve ser maior que zero")
    BigDecimal valor,
    
    @NotBlank 
    String requestKey // UUID único gerado no clique do botão
) {}
