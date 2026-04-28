package br.com.ecomel.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para criação e edição de usuários.
 * O ID é gerado automaticamente pelo banco, por isso não consta aqui.
 * O e-mail é opcional conforme as novas regras da ECOMEL.
 */
public record UsuarioRequest(
    @NotBlank(message = "O nome é obrigatório") 
    String nome,

    @Email(message = "E-mail inválido") 
    String email, // Removido @NotBlank para tornar opcional

    @NotBlank(message = "A senha é obrigatória")
    //@Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
    String senha
) {}
