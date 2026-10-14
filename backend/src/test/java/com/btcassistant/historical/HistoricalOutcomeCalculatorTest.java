package com.btcassistant.historical;

import com.btcassistant.market.Candle;
import com.btcassistant.market.CandleEntity;
import com.btcassistant.market.CandleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricalOutcomeCalculatorTest {

    private static final Instant EVENT_TIME = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID EVENT_ID = UUID.randomUUID();

    @Mock
    private CandleRepository candleRepository;

    private HistoricalOutcomeCalculator calculator;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        calculator = new HistoricalOutcomeCalculator(candleRepository, new ResearchOutcomeProperties());
    }

    private static Candle candleAt(int minuteOffset, String high, String low, String close) {
        Instant openTime = EVENT_TIME.plusSeconds(60L * minuteOffset);
        return new Candle("BTCUSDT", "1m", openTime, openTime.plusSeconds(59),
                new BigDecimal(close), new BigDecimal(high), new BigDecimal(low), new BigDecimal(close),
                new BigDecimal("10"), new BigDecimal("1000"), 1, "BINANCE_WS");
    }

    @Test
    void returnsEmptyWhenHorizonHasNotElapsedYet() {
        when(candleRepository.findBySymbolAndIntervalAndOpenTime("BTCUSDT", "1m", EVENT_TIME.plusSeconds(300)))
                .thenReturn(Optional.empty());

        Optional<HistoricalOutcomeResult> result = calculator.calculate(EVENT_ID, "BTCUSDT", "1m", EVENT_TIME, new BigDecimal("100"), "5m");

        assertThat(result).isEmpty();
    }

    @Test
    void computesFutureReturnMfeMaeAndTargetBeforeStop() {
        Candle c1 = candleAt(1, "100.5", "99.5", "100.2");
        Candle c2 = candleAt(2, "101.2", "99.8", "101.0");
        Candle c3 = candleAt(3, "101.5", "100.5", "101.3");
        Candle c4 = candleAt(4, "101.8", "100.8", "101.5");
        Candle c5 = candleAt(5, "102.0", "101.0", "101.8");

        when(candleRepository.findBySymbolAndIntervalAndOpenTime("BTCUSDT", "1m", EVENT_TIME.plusSeconds(300)))
                .thenReturn(Optional.of(CandleEntity.from(c5)));
        when(candleRepository.findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                "BTCUSDT", "1m", EVENT_TIME.plusSeconds(1), EVENT_TIME.plusSeconds(300)))
                .thenReturn(List.of(c1, c2, c3, c4, c5).stream().map(CandleEntity::from).toList());

        HistoricalOutcomeResult result = calculator.calculate(EVENT_ID, "BTCUSDT", "1m", EVENT_TIME, new BigDecimal("100"), "5m")
                .orElseThrow();

        assertThat(result.futureReturn()).isEqualByComparingTo("0.018");
        assertThat(result.mfe()).isEqualByComparingTo("0.02");
        assertThat(result.mae()).isEqualByComparingTo("-0.005");
        assertThat(result.targetBeforeStop()).isTrue();
        assertThat(result.timeToTarget()).isEqualTo(Duration.ofMinutes(2).plusSeconds(59));
        assertThat(result.timeToStop()).isNull();
        assertThat(result.evaluationEnd()).isEqualTo(EVENT_TIME.plusSeconds(300));
    }

    @Test
    void stopHitBeforeTargetResultsInTargetBeforeStopFalse() {
        Candle c1 = candleAt(1, "100.2", "98.9", "99.0");
        Candle c2 = candleAt(2, "100.3", "99.5", "100.0");
        Candle c3 = candleAt(3, "100.4", "99.6", "100.1");
        Candle c4 = candleAt(4, "100.5", "99.7", "100.2");
        Candle c5 = candleAt(5, "100.6", "99.8", "100.3");

        when(candleRepository.findBySymbolAndIntervalAndOpenTime("BTCUSDT", "1m", EVENT_TIME.plusSeconds(300)))
                .thenReturn(Optional.of(CandleEntity.from(c5)));
        when(candleRepository.findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                "BTCUSDT", "1m", EVENT_TIME.plusSeconds(1), EVENT_TIME.plusSeconds(300)))
                .thenReturn(List.of(c1, c2, c3, c4, c5).stream().map(CandleEntity::from).toList());

        HistoricalOutcomeResult result = calculator.calculate(EVENT_ID, "BTCUSDT", "1m", EVENT_TIME, new BigDecimal("100"), "5m")
                .orElseThrow();

        assertThat(result.targetBeforeStop()).isFalse();
        assertThat(result.timeToStop()).isNotNull();
    }

    @Test
    void neitherTargetNorStopHitLeavesTargetBeforeStopNull() {
        Candle c1 = candleAt(1, "100.2", "99.8", "100.0");
        Candle c2 = candleAt(2, "100.3", "99.7", "100.1");
        Candle c3 = candleAt(3, "100.1", "99.6", "99.9");
        Candle c4 = candleAt(4, "100.2", "99.7", "100.0");
        Candle c5 = candleAt(5, "100.3", "99.8", "100.1");

        when(candleRepository.findBySymbolAndIntervalAndOpenTime("BTCUSDT", "1m", EVENT_TIME.plusSeconds(300)))
                .thenReturn(Optional.of(CandleEntity.from(c5)));
        when(candleRepository.findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                "BTCUSDT", "1m", EVENT_TIME.plusSeconds(1), EVENT_TIME.plusSeconds(300)))
                .thenReturn(List.of(c1, c2, c3, c4, c5).stream().map(CandleEntity::from).toList());

        HistoricalOutcomeResult result = calculator.calculate(EVENT_ID, "BTCUSDT", "1m", EVENT_TIME, new BigDecimal("100"), "5m")
                .orElseThrow();

        assertThat(result.targetBeforeStop()).isNull();
        assertThat(result.timeToTarget()).isNull();
        assertThat(result.timeToStop()).isNull();
    }
}
