package br.com.ecomel.dto.response;

import java.math.BigDecimal;
import java.math.BigInteger;

public record CarteiraResponse(
    String codigoEndereco,
    BigDecimal tokenEcomel,     // Quantidade inteira de tokens ECOMEL (substitui saldoBaseECM)
    BigDecimal saldoRealECM,    // Valor valorizado (tokenEcomel * Indice)
    BigDecimal saldoFavos,      // Ativo negociável
    BigDecimal favosEmOrdem,    // FAVOS bloqueados em ordens de venda abertas
    BigDecimal ecmEmOrdem,      // ECM bloqueada em ordens de compra abertas
    BigDecimal indiceAtual      // Para transparência do cálculo
) {
    // Construtor explícito para garantir a ordem e facilitar chamadas
    public CarteiraResponse(
            String codigoEndereco,
            BigDecimal tokenEcomel,
            BigDecimal saldoRealECM,
            BigDecimal saldoFavos,
            BigDecimal favosEmOrdem,
            BigDecimal ecmEmOrdem,
            BigDecimal indiceAtual) {
        this.codigoEndereco = codigoEndereco;
        this.tokenEcomel = tokenEcomel;
        this.saldoRealECM = saldoRealECM;
        this.saldoFavos = saldoFavos;
        this.favosEmOrdem = favosEmOrdem;
        this.ecmEmOrdem = ecmEmOrdem;
        this.indiceAtual = indiceAtual;
    }
}
