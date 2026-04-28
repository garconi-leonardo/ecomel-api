package br.com.ecomel.dto.response;

public record JwtAuthenticationResponse(
    String accessToken,
    String tokenType
) {
    // Construtor auxiliar para facilitar
    public JwtAuthenticationResponse(String accessToken) {
        this(accessToken, "Bearer");
    }
}

