package br.com.ecomel.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public class CalculoFinanceiroUtils {

    public static final int ESCALA_ECM = 18;
    public static final int ESCALA_FAVOS = 8;

    // SEMPRE usamos DOWN para garantir que o sistema nunca pague mais do que deve
    public static BigDecimal formatarEcm(BigDecimal valor) {
        return valor.setScale(ESCALA_ECM, RoundingMode.DOWN);
    }

    public static BigDecimal dividirEcm(BigDecimal dividendo, BigDecimal divisor) {
        return dividendo.divide(divisor, ESCALA_ECM, RoundingMode.DOWN);
    }

    /**
     * Converte um BigDecimal em BigInteger truncando (arredonda SEMPRE para menos).
     * Usado para o campo tokenEcomel, que sempre é armazenado como inteiro.
     */
    public static BigInteger toTokenEcomel(BigDecimal valor) {
        if (valor == null) return BigInteger.ZERO;
        return valor.setScale(0, RoundingMode.DOWN).toBigInteger();
    }
}
