package com.btcassistant.feature;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

/** Utilitários estatísticos compartilhados entre calculadores (média/desvio padrão amostral). */
final class StatisticsSupport {

    private StatisticsSupport() {
    }

    static BigDecimal mean(List<BigDecimal> values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            sum = sum.add(value);
        }
        return sum.divide(BigDecimal.valueOf(values.size()), MathContext.DECIMAL64);
    }

    /** Desvio padrão amostral (divisor {@code n-1}); requer ao menos 2 valores. */
    static BigDecimal sampleStdDev(List<BigDecimal> values) {
        BigDecimal mean = mean(values);
        BigDecimal sumSquaredDiff = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            BigDecimal diff = value.subtract(mean);
            sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
        }
        BigDecimal variance = sumSquaredDiff.divide(BigDecimal.valueOf(values.size() - 1), MathContext.DECIMAL64);
        return sqrt(variance);
    }

    /** Raiz quadrada por Newton-Raphson — evita dependência de {@code double} para precisão financeira. */
    static BigDecimal sqrt(BigDecimal value) {
        if (value.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal x = BigDecimal.valueOf(Math.sqrt(value.doubleValue()));
        for (int i = 0; i < 8; i++) {
            x = x.add(value.divide(x, MathContext.DECIMAL64)).divide(BigDecimal.TWO, MathContext.DECIMAL64);
        }
        return x;
    }
}
