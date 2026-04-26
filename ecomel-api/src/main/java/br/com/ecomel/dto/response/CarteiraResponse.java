package br.com.ecomel.dto.response;

import java.math.BigDecimal;

public record CarteiraResponse(
    String codigoEndereco,
    BigDecimal saldoBaseECM, 	// Valor técnico
    BigDecimal saldoRealECM,    // Valor valorizado (Base * Indice)
    BigDecimal saldoFavos,      // Ativo negociável
    BigDecimal favosEmOrdem,    // FAVOS bloqueados em ordens de venda abertas
    BigDecimal ecmEmOrdem,      // ECM bloqueada em ordens de compra abertas
    BigDecimal indiceAtual		// Para transparência do cálculo
) {
    // Construtor explícito para garantir a ordem e facilitar chamadas
    public CarteiraResponse(
            String codigoEndereco,
            BigDecimal saldoBaseECM,
            BigDecimal saldoRealECM,
            BigDecimal saldoFavos,
            BigDecimal favosEmOrdem,
            BigDecimal ecmEmOrdem,
            BigDecimal indiceAtual) {
        this.codigoEndereco = codigoEndereco;
        this.saldoBaseECM = saldoBaseECM;
        this.saldoRealECM = saldoRealECM;
        this.saldoFavos = saldoFavos;
        this.favosEmOrdem = favosEmOrdem;
        this.ecmEmOrdem = ecmEmOrdem;
        this.indiceAtual = indiceAtual;
    }
}
