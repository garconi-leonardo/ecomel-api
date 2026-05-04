package br.com.ecomel.dto.response;

import java.math.BigDecimal;

public record CarteiraResponse(

    String codigoCarteira,	//

    BigDecimal tokenEcomel,     // Quantidade TOTAL de tokens ECOMEL (inclui disponível + bloqueado)

    BigDecimal saldoEcomelDisponivel, // ECM livre para uso (tokenEcomel - bloqueado)

    BigDecimal saldoRealECM,    // Valor valorizado (tokenEcomel * indiceAtual)

    BigDecimal saldoFavos,      // Quantidade TOTAL de FAVOS (inclui disponível + bloqueado)

    BigDecimal saldoFavosDisponivel, // FAVOS livres para negociação

    BigDecimal favosEmOrdem,    // FAVOS bloqueados em ordens de venda abertas

    BigDecimal ecmEmOrdem,      // ECM bloqueado em ordens de compra abertas

    BigDecimal indiceAtual      // Índice atual para transparência do cálculo

) {

    // Construtor explícito para garantir a ordem e facilitar chamadas
    public CarteiraResponse(
            String codigoCarteira,
            BigDecimal tokenEcomel,
            BigDecimal saldoEcomelDisponivel,
            BigDecimal saldoRealECM,
            BigDecimal saldoFavos,
            BigDecimal saldoFavosDisponivel,
            BigDecimal favosEmOrdem,
            BigDecimal ecmEmOrdem,
            BigDecimal indiceAtual
    ) {
        this.codigoCarteira = codigoCarteira;
        this.tokenEcomel = tokenEcomel;
        this.saldoEcomelDisponivel = saldoEcomelDisponivel;
        this.saldoRealECM = saldoRealECM;
        this.saldoFavos = saldoFavos;
        this.saldoFavosDisponivel = saldoFavosDisponivel;
        this.favosEmOrdem = favosEmOrdem;
        this.ecmEmOrdem = ecmEmOrdem;
        this.indiceAtual = indiceAtual;
    }
}
