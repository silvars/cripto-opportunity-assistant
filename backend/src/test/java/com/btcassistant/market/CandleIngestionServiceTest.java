package com.btcassistant.market;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandleIngestionServiceTest {

    @Mock
    private CandlePersister candlePersister;
    @Mock
    private CandleRepository candleRepository;
    @Mock
    private CandlePublisher candlePublisher;
    @Mock
    private GapRecoveryService gapRecoveryService;
    @Mock
    private MarketStatusRegistry statusRegistry;

    private CandleIngestionService service;

    @BeforeEach
    void setUp() {
        service = new CandleIngestionService(candlePersister, candleRepository, candlePublisher, gapRecoveryService, statusRegistry);
    }

    private static Candle candle(Instant openTime) {
        return new Candle("BTCUSDT", "1m", openTime, openTime.plusSeconds(59),
                new BigDecimal("100"), new BigDecimal("110"), new BigDecimal("90"), new BigDecimal("105"),
                new BigDecimal("10"), new BigDecimal("1000"), 50, "BINANCE_WS");
    }

    @Test
    void publishesAndUpdatesStatusWhenNewCandlePersisted() {
        Candle candle = candle(Instant.parse("2026-01-01T00:01:00Z"));
        when(candleRepository.findTopBySymbolAndIntervalOrderByOpenTimeDesc("BTCUSDT", "1m")).thenReturn(Optional.empty());
        when(candlePersister.persist(candle)).thenReturn(true);

        service.onCandle(candle);

        verify(candlePublisher).publish(candle);
    }

    @Test
    void doesNotPublishWhenCandleNotPersisted() {
        Candle candle = candle(Instant.parse("2026-01-01T00:01:00Z"));
        when(candleRepository.findTopBySymbolAndIntervalOrderByOpenTimeDesc("BTCUSDT", "1m")).thenReturn(Optional.empty());
        when(candlePersister.persist(candle)).thenReturn(false);

        service.onCandle(candle);

        verify(candlePublisher, never()).publish(any());
    }

    @Test
    void triggersGapRecoveryWhenStepLargerThanInterval() {
        Instant previousOpenTime = Instant.parse("2026-01-01T00:00:00Z");
        Instant currentOpenTime = Instant.parse("2026-01-01T00:05:00Z");
        CandleEntity previous = CandleEntity.from(candle(previousOpenTime));
        when(candleRepository.findTopBySymbolAndIntervalOrderByOpenTimeDesc("BTCUSDT", "1m")).thenReturn(Optional.of(previous));
        Candle current = candle(currentOpenTime);
        when(candlePersister.persist(current)).thenReturn(true);

        service.onCandle(current);

        verify(gapRecoveryService).recover("BTCUSDT", "1m", previousOpenTime, currentOpenTime);
    }

    @Test
    void doesNotTriggerGapRecoveryWhenStepMatchesInterval() {
        Instant previousOpenTime = Instant.parse("2026-01-01T00:00:00Z");
        Instant currentOpenTime = Instant.parse("2026-01-01T00:01:00Z");
        CandleEntity previous = CandleEntity.from(candle(previousOpenTime));
        when(candleRepository.findTopBySymbolAndIntervalOrderByOpenTimeDesc("BTCUSDT", "1m")).thenReturn(Optional.of(previous));
        Candle current = candle(currentOpenTime);
        when(candlePersister.persist(current)).thenReturn(true);

        service.onCandle(current);

        verify(gapRecoveryService, never()).recover(any(), any(), any(), any());
    }
}
