package com.btcassistant.feature;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static com.btcassistant.feature.FeatureTestSupport.candleAt;
import static org.assertj.core.api.Assertions.assertThat;

class PriceDeltaMomentumCalculatorTest {

    private final PriceDeltaMomentumCalculator calculator = new PriceDeltaMomentumCalculator();

    @Test
    void calculatesAbsolutePriceDeltaAgainstExactHorizonCandle() {
        RollingWindow window = RollingWindow.of(List.of(
                candleAt(0, "100"), candleAt(1, "101"), candleAt(2, "102"), candleAt(3, "103"),
                candleAt(4, "104"), candleAt(5, "108")));

        assertThat(calculator.calculate(window, Duration.ofMinutes(5))).isEqualByComparingTo("8");
    }

    @Test
    void returnsNullWhenHorizonCandleNotInWindow() {
        RollingWindow window = RollingWindow.of(List.of(candleAt(0, "100"), candleAt(1, "101")));

        assertThat(calculator.calculate(window, Duration.ofMinutes(10))).isNull();
    }
}
