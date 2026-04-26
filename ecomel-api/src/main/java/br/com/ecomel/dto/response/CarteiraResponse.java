package br.com.ecomel.dto.response;

import java.math.BigDecimal;

public record CarteiraResponse(
    Long usuarioId,
    BigDecimal saldoBaseECM,    // Valor técnico
    BigDecimal saldoRealECM,    // Valor valorizado (Base * Indice)
    BigDecimal saldoFavos,      // Ativo negociável
    BigDecimal indiceAtual      // Para transparência do cálculo
) {}