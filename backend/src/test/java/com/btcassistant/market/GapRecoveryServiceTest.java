package com.btcassistant.market;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GapRecoveryServiceTest {

    @Mock
    private MarketDataProvider marketDataProvider;
    @Mock
    private CandlePersister candlePersister;
    @Mock
    private MarketProperties marketProperties;
    @Mock
    private MarketStatusRegistry statusRegistry;

    private GapRecoveryService service;

    @BeforeEach
    void setUp() {
        service = new GapRecoveryService(marketDataProvider, candlePersister, marketProperties, statusRegistry);
    }

    private static Candle candle(Instant openTime) {
        return new Candle("BTCUSDT", "1m", openTime, openTime.plusSeconds(59),
                new BigDecimal("100"), new BigDecimal("110"), new BigDecimal("90"), new BigDecimal("105"),
                new BigDecimal("10"), new BigDecimal("1000"), 50, "BINANCE_REST");
    }

    @Test
    void doesNothingWhenInstrumentNotConfigured() {
        when(marketProperties.findEnabled("BTCUSDT", "1m")).thenReturn(null);

        service.recover("BTCUSDT", "1m", Instant.EPOCH, Instant.EPOCH.plusSeconds(60));

        verify(marketDataProvider, never()).fetchHistorical(any(), any(), any());
    }

    @Test
    void fetchesAndPersistsMissingCandlesThenRefreshesStatus() {
        Instrument instrument = new Instrument("BTCUSDT", "BTC", "USDT", "BINANCE", "1m", true);
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = from.plusSeconds(120);
        Candle candle1 = candle(from);
        Candle candle2 = candle(from.plusSeconds(60));

        when(marketProperties.findEnabled("BTCUSDT", "1m")).thenReturn(instrument);
        when(marketDataProvider.fetchHistorical(instrument, from, to)).thenReturn(List.of(candle1, candle2));
        when(candlePersister.persist(candle1)).thenReturn(true);
        when(candlePersister.persist(candle2)).thenReturn(false);

        service.recover("BTCUSDT", "1m", from, to);

        verify(candlePersister).persist(candle1);
        verify(candlePersister).persist(candle2);
        verify(statusRegistry).refreshCounts("BTCUSDT", "1m");
    }
}
