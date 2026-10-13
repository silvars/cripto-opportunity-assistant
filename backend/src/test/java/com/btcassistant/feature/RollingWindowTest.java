package com.btcassistant.feature;

import com.btcassistant.market.Candle;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.btcassistant.feature.FeatureTestSupport.BASE_TIME;
import static com.btcassistant.feature.FeatureTestSupport.candleAt;
import static org.assertj.core.api.Assertions.assertThat;

class RollingWindowTest {

    private static final List<Candle> CANDLES = List.of(
            candleAt(0, "100"), candleAt(1, "101"), candleAt(2, "102"), candleAt(3, "103"));

    @Test
    void referenceIsLastCandle() {
        assertThat(RollingWindow.of(CANDLES).reference().close().toString()).isEqualTo("103");
    }

    @Test
    void atFindsExactOpenTimeOnly() {
        RollingWindow window = RollingWindow.of(CANDLES);

        assertThat(window.at(BASE_TIME.plusSeconds(60))).isPresent();
        assertThat(window.at(BASE_TIME.plusSeconds(90))).isEmpty();
    }

    @Test
    void sinceIncludesCandlesFromBoundaryInclusive() {
        RollingWindow window = RollingWindow.of(CANDLES);

        List<Candle> since = window.since(BASE_TIME.plus(Duration.ofMinutes(1)));

        assertThat(since).hasSize(3);
        assertThat(since.get(0).close().toString()).isEqualTo("101");
    }

    @Test
    void lastNReturnsFewerCandlesWhenWindowSmallerThanN() {
        RollingWindow window = RollingWindow.of(CANDLES);

        assertThat(window.lastN(2)).hasSize(2);
        assertThat(window.lastN(10)).hasSize(4);
    }

    @Test
    void spanIsDurationBetweenFirstAndReferenceCandle() {
        assertThat(RollingWindow.of(CANDLES).span()).isEqualTo(Duration.ofMinutes(3));
    }
}
