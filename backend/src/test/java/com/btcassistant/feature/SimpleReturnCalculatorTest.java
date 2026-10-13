package com.btcassistant.feature;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static com.btcassistant.feature.FeatureTestSupport.candleAt;
import static org.assertj.core.api.Assertions.assertThat;

class SimpleReturnCalculatorTest {

    private final SimpleReturnCalculator calculator = new SimpleReturnCalculator();

    @Test
    void calculatesPercentageReturnAgainstExactHorizonCandle() {
        RollingWindow window = RollingWindow.of(List.of(
                candleAt(0, "100"), candleAt(1, "101"), candleAt(2, "102"), candleAt(3, "103"),
                candleAt(4, "104"), candleAt(5, "105")));

        assertThat(calculator.calculate(window, Duration.ofMinutes(5)))
                .isEqualByComparingTo("0.05");
        assertThat(calculator.calculate(window, Duration.ofMinutes(1)))
                .isEqualByComparingTo(new java.math.BigDecimal("105").subtract(new java.math.BigDecimal("104"))
                        .divide(new java.math.BigDecimal("104"), java.math.MathContext.DECIMAL64));
    }

    @Test
    void returnsNullWhenHorizonCandleNotInWindow() {
        RollingWindow window = RollingWindow.of(List.of(candleAt(0, "100"), candleAt(1, "101")));

        assertThat(calculator.calculate(window, Duration.ofMinutes(10))).isNull();
    }
}
