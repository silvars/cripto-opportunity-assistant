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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandlePersisterTest {

    @Mock
    private CandleRepository candleRepository;
    @Mock
    private MarketStatusRegistry statusRegistry;

    private CandlePersister persister;

    @BeforeEach
    void setUp() {
        persister = new CandlePersister(candleRepository, statusRegistry);
    }

    private static Candle candle(Instant openTime) {
        return new Candle("BTCUSDT", "1m", openTime, openTime.plusSeconds(59),
                new BigDecimal("100"), new BigDecimal("110"), new BigDecimal("90"), new BigDecimal("105"),
                new BigDecimal("10"), new BigDecimal("1000"), 50, "BINANCE_REST");
    }

    @Test
    void rejectsInvalidCandleWithoutPersisting() {
        Candle invalid = new Candle("BTCUSDT", "1m", Instant.EPOCH, Instant.EPOCH,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, 0, "BINANCE_REST");

        boolean persisted = persister.persist(invalid);

        assertThat(persisted).isFalse();
        verify(candleRepository, never()).save(any());
        verify(statusRegistry, never()).candlePersisted(any(), any(), any());
    }

    @Test
    void skipsAlreadyExistingCandle() {
        Instant openTime = Instant.parse("2026-01-01T00:00:00Z");
        Candle candle = candle(openTime);
        when(candleRepository.findBySymbolAndIntervalAndOpenTime("BTCUSDT", "1m", openTime))
                .thenReturn(Optional.of(CandleEntity.from(candle)));

        boolean persisted = persister.persist(candle);

        assertThat(persisted).isFalse();
        verify(candleRepository, never()).save(any());
    }

    @Test
    void persistsNewValidCandleAndUpdatesStatus() {
        Instant openTime = Instant.parse("2026-01-01T00:00:00Z");
        Candle candle = candle(openTime);
        when(candleRepository.findBySymbolAndIntervalAndOpenTime("BTCUSDT", "1m", openTime))
                .thenReturn(Optional.empty());

        boolean persisted = persister.persist(candle);

        assertThat(persisted).isTrue();
        verify(candleRepository).save(any(CandleEntity.class));
        verify(statusRegistry).candlePersisted("BTCUSDT", "1m", openTime);
    }
}
