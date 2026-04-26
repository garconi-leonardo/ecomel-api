package br.com.ecomel.util;

import java.math.BigDecimal;
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
}
