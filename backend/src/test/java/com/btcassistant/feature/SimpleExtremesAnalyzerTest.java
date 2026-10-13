package com.btcassistant.feature;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleExtremesAnalyzerTest {

    private final SimpleExtremesAnalyzer analyzer = new SimpleExtremesAnalyzer();

    @Test
    void distanceFromRecentHighIsNegativeWhenBelowWindowHigh() {
        RollingWindow window = RollingWindow.of(List.of(
                FeatureTestSupport.candleAt(0, "100", "120", "95", "1"),
                FeatureTestSupport.candleAt(1, "110", "110", "105", "1")));

        // highest high = 120, close agora = 110 -> (110-120)/120
        assertThat(analyzer.distanceFromRecentHigh(window, Duration.ofMinutes(5)))
                .isEqualByComparingTo(new java.math.BigDecimal("110").subtract(new java.math.BigDecimal("120"))
                        .divide(new java.math.BigDecimal("120"), java.math.MathContext.DECIMAL64));
    }

    @Test
    void distanceFromRecentHighIsZeroWhenAtNewHigh() {
        RollingWindow window = RollingWindow.of(List.of(
                FeatureTestSupport.candleAt(0, "100", "100", "95", "1"),
                FeatureTestSupport.candleAt(1, "110", "110", "105", "1")));

        assertThat(analyzer.distanceFromRecentHigh(window, Duration.ofMinutes(5))).isEqualByComparingTo("0");
    }

    @Test
    void distanceFromRecentLowIsPositiveWhenAboveWindowLow() {
        RollingWindow window = RollingWindow.of(List.of(
                FeatureTestSupport.candleAt(0, "100", "105", "80", "1"),
                FeatureTestSupport.candleAt(1, "110", "112", "108", "1")));

        // lowest low = 80, close agora = 110 -> (110-80)/80 = 0.375
        assertThat(analyzer.distanceFromRecentLow(window, Duration.ofMinutes(5))).isEqualByComparingTo("0.375");
    }
}
