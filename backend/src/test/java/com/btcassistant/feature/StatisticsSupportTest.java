package com.btcassistant.feature;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StatisticsSupportTest {

    @Test
    void meanOfKnownValues() {
        List<BigDecimal> values = List.of(new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"), new BigDecimal("4"));

        assertThat(StatisticsSupport.mean(values)).isEqualByComparingTo("2.5");
    }

    @Test
    void sampleStdDevOfKnownValues() {
        // 2, 4, 4, 4, 5, 5, 7, 9 -> média 5, variância amostral (n-1=7) = 32/7, stdev = sqrt(32/7) ≈ 2.138089935
        List<BigDecimal> values = List.of(
                new BigDecimal("2"), new BigDecimal("4"), new BigDecimal("4"), new BigDecimal("4"),
                new BigDecimal("5"), new BigDecimal("5"), new BigDecimal("7"), new BigDecimal("9"));

        BigDecimal stdDev = StatisticsSupport.sampleStdDev(values);

        assertThat(stdDev.doubleValue()).isCloseTo(2.138089935299395, within(1e-9));
    }

    @Test
    void sqrtOfZeroIsZero() {
        assertThat(StatisticsSupport.sqrt(BigDecimal.ZERO)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void sqrtOfKnownPerfectSquare() {
        assertThat(StatisticsSupport.sqrt(new BigDecimal("144")).doubleValue()).isCloseTo(12.0, within(1e-9));
    }
}
