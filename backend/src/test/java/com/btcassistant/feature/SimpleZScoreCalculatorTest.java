package com.btcassistant.feature;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SimpleZScoreCalculatorTest {

    private final SimpleZScoreCalculator calculator = new SimpleZScoreCalculator();

    @Test
    void calculatesZScoreAgainstSeriesMeanAndStdDev() {
        // série [2,4,4,4,5,5,7,9]: média 5, stdev amostral ≈ 2.138089935
        List<BigDecimal> series = List.of(
                new BigDecimal("2"), new BigDecimal("4"), new BigDecimal("4"), new BigDecimal("4"),
                new BigDecimal("5"), new BigDecimal("5"), new BigDecimal("7"), new BigDecimal("9"));

        BigDecimal zscore = calculator.zscore(new BigDecimal("9"), series);

        assertThat(zscore.doubleValue()).isCloseTo((9.0 - 5.0) / 2.138089935299395, within(1e-6));
    }

    @Test
    void returnsNullWhenSeriesHasFewerThanTwoValues() {
        assertThat(calculator.zscore(new BigDecimal("1"), List.of(new BigDecimal("1")))).isNull();
    }

    @Test
    void returnsNullWhenStdDevIsZero() {
        List<BigDecimal> series = List.of(new BigDecimal("5"), new BigDecimal("5"), new BigDecimal("5"));

        assertThat(calculator.zscore(new BigDecimal("5"), series)).isNull();
    }
}
