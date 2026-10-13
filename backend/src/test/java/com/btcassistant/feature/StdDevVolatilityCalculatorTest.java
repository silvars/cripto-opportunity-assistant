package com.btcassistant.feature;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static com.btcassistant.feature.FeatureTestSupport.candleAt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StdDevVolatilityCalculatorTest {

    private final StdDevVolatilityCalculator calculator = new StdDevVolatilityCalculator();

    @Test
    void calculatesSampleStdDevOfConsecutiveReturns() {
        // closes 100 -> 110 (+10%) -> 99 (-10%): retornos [0.10, -0.10], média 0, stdev amostral = sqrt(0.02)
        RollingWindow window = RollingWindow.of(List.of(
                candleAt(0, "100"), candleAt(1, "110"), candleAt(2, "99")));

        java.math.BigDecimal result = calculator.calculate(window, Duration.ofMinutes(15));

        assertThat(result.doubleValue()).isCloseTo(Math.sqrt(0.02), within(1e-9));
    }

    @Test
    void returnsNullWhenFewerThanThreeCandlesInWindow() {
        RollingWindow window = RollingWindow.of(List.of(candleAt(0, "100"), candleAt(1, "110")));

        assertThat(calculator.calculate(window, Duration.ofMinutes(15))).isNull();
    }

    @Test
    void returnsNullWhenHorizonExcludesEnoughHistory() {
        RollingWindow window = RollingWindow.of(List.of(
                candleAt(0, "100"), candleAt(1, "110"), candleAt(2, "99"), candleAt(3, "105")));

        // horizon de 1 minuto só inclui o candle de referência + o anterior -> 1 retorno, insuficiente
        assertThat(calculator.calculate(window, Duration.ofMinutes(1))).isNull();
    }
}
