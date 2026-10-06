package com.btcassistant.market;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketStatusRegistryTest {

    @Mock
    private CandlePublisher candlePublisher;
    @Mock
    private CandleRepository candleRepository;

    private MarketStatusRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new MarketStatusRegistry(candlePublisher, candleRepository);
    }

    private static Candle candle(Instant openTime) {
        return new Candle("BTCUSDT", "1m", openTime, openTime.plusSeconds(59),
                new BigDecimal("100"), new BigDecimal("110"), new BigDecimal("90"), new BigDecimal("105"),
                new BigDecimal("10"), new BigDecimal("1000"), 50, "BINANCE_REST");
    }

    @Test
    void startsInInitializingPhase() {
        MarketStatus status = registry.snapshot("BTCUSDT");

        assertThat(status.phase()).isEqualTo(MarketPhase.INITIALIZING);
        assertThat(status.websocketConnected()).isFalse();
        assertThat(status.historicalDataReady()).isFalse();
    }

    @Test
    void tracksPhaseTransitions() {
        registry.updatePhase("BTCUSDT", MarketPhase.BACKFILLING);

        assertThat(registry.snapshot("BTCUSDT").phase()).isEqualTo(MarketPhase.BACKFILLING);
    }

    @Test
    void marksHistoricalReady() {
        Instant requiredStart = Instant.parse("2026-01-01T00:00:00Z");

        registry.historicalReady("BTCUSDT", requiredStart);

        MarketStatus status = registry.snapshot("BTCUSDT");
        assertThat(status.historicalDataReady()).isTrue();
        assertThat(status.phase()).isEqualTo(MarketPhase.HISTORICAL_DATA_READY);
        assertThat(status.requiredHistoryStart()).isEqualTo(requiredStart);
    }

    @Test
    void tracksEarliestAndLatestCandleAcrossUpdates() {
        when(candleRepository.countBySymbolAndInterval("BTCUSDT", "1m")).thenReturn(1L, 2L);
        Instant first = Instant.parse("2026-01-01T00:05:00Z");
        Instant second = Instant.parse("2026-01-01T00:01:00Z");

        registry.candlePersisted("BTCUSDT", "1m", first);
        registry.candlePersisted("BTCUSDT", "1m", second);

        MarketStatus status = registry.snapshot("BTCUSDT");
        assertThat(status.earliestCandle()).isEqualTo(second);
        assertThat(status.latestCandle()).isEqualTo(first);
        assertThat(status.candleCount()).isEqualTo(2L);
    }

    @Test
    void refreshCountsUsesAggregateQueries() {
        Instant earliest = Instant.parse("2026-01-01T00:00:00Z");
        Instant latest = Instant.parse("2026-01-02T00:00:00Z");
        when(candleRepository.countBySymbolAndInterval("BTCUSDT", "1m")).thenReturn(500L);
        when(candleRepository.findTopBySymbolAndIntervalOrderByOpenTimeAsc("BTCUSDT", "1m"))
                .thenReturn(Optional.of(CandleEntity.from(candle(earliest))));
        when(candleRepository.findTopBySymbolAndIntervalOrderByOpenTimeDesc("BTCUSDT", "1m"))
                .thenReturn(Optional.of(CandleEntity.from(candle(latest))));

        registry.refreshCounts("BTCUSDT", "1m");

        MarketStatus status = registry.snapshot("BTCUSDT");
        assertThat(status.candleCount()).isEqualTo(500L);
        assertThat(status.earliestCandle()).isEqualTo(earliest);
        assertThat(status.latestCandle()).isEqualTo(latest);
    }
}
