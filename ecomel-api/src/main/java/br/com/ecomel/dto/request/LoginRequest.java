package br.com.ecomel.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
	@NotBlank String login, // Aceita e-mail OU código da carteira (AAA000)
    @NotBlank String senha,
    @NotBlank String requestKey // UUID único gerado no clique do botão
) {}