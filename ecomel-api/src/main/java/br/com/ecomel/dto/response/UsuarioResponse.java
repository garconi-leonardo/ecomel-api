package br.com.ecomel.dto.response;

import java.time.LocalDateTime;

public record UsuarioResponse(
    Long id,
    String nome,
    String email,
    LocalDateTime criadoEm,
    CarteiraResponse carteira // Referência direta ao objeto Carteira
) {}
