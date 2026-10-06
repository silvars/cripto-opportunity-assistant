package com.btcassistant.market;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricalBackfillServiceTest {

    private static final Instrument INSTRUMENT = new Instrument("BTCUSDT", "BTC", "USDT", "BINANCE", "1m", true);

    @Mock
    private MarketDataProvider marketDataProvider;
    @Mock
    private HistoricalCoverageChecker coverageChecker;
    @Mock
    private CandlePersister candlePersister;
    @Mock
    private MarketStatusRegistry statusRegistry;

    private HistoricalBackfillService service(int maxRetries) {
        return new HistoricalBackfillService(marketDataProvider, coverageChecker, candlePersister, statusRegistry, maxRetries);
    }

    private static Candle candle(Instant openTime) {
        return new Candle("BTCUSDT", "1m", openTime, openTime.plusSeconds(59),
                new BigDecimal("100"), new BigDecimal("110"), new BigDecimal("90"), new BigDecimal("105"),
                new BigDecimal("10"), new BigDecimal("1000"), 50, "BINANCE_REST");
    }

    @Test
    void skipsWhenBackfillDisabled() {
        MarketProperties.HistoricalBackfillConfig config = new MarketProperties.HistoricalBackfillConfig();
        config.setEnabled(false);

        service(3).ensureHistoricalData(INSTRUMENT, config);

        verify(marketDataProvider, never()).fetchHistorical(any(), any(), any());
        verify(statusRegistry).historicalReady(eq("BTCUSDT"), isNull());
    }

    @Test
    void skipsBackfillWhenAlreadyComplete() {
        MarketProperties.HistoricalBackfillConfig config = new MarketProperties.HistoricalBackfillConfig();
        config.setEnabled(true);
        config.setPeriod("1-days");
        when(coverageChecker.findMissingRanges(eq("BTCUSDT"), eq("1m"), any(), any())).thenReturn(List.of());

        service(3).ensureHistoricalData(INSTRUMENT, config);

        verify(marketDataProvider, never()).fetchHistorical(any(), any(), any());
        verify(statusRegistry).historicalReady(eq("BTCUSDT"), any(Instant.class));
    }

    @Test
    void fetchesAndPersistsMissingRangeUntilComplete() {
        MarketProperties.HistoricalBackfillConfig config = new MarketProperties.HistoricalBackfillConfig();
        config.setEnabled(true);
        config.setPeriod("1-days");

        HistoricalRange missingRange = new HistoricalRange(Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
        when(coverageChecker.findMissingRanges(eq("BTCUSDT"), eq("1m"), any(), any()))
                .thenReturn(List.of(missingRange))
                .thenReturn(List.of());

        Candle candle = candle(Instant.EPOCH);
        when(marketDataProvider.fetchHistorical(eq(INSTRUMENT), any(), any())).thenReturn(List.of(candle));
        when(candlePersister.persist(candle)).thenReturn(true);

        service(3).ensureHistoricalData(INSTRUMENT, config);

        verify(marketDataProvider, times(1)).fetchHistorical(eq(INSTRUMENT), any(), any());
        verify(statusRegistry).refreshCounts("BTCUSDT", "1m");
        verify(statusRegistry).historicalReady(eq("BTCUSDT"), any(Instant.class));
    }

    @Test
    void givesUpImmediatelyWhenMaxRetriesIsZeroAndStillIncomplete() {
        MarketProperties.HistoricalBackfillConfig config = new MarketProperties.HistoricalBackfillConfig();
        config.setEnabled(true);
        config.setPeriod("1-days");

        HistoricalRange missingRange = new HistoricalRange(Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
        when(coverageChecker.findMissingRanges(eq("BTCUSDT"), eq("1m"), any(), any())).thenReturn(List.of(missingRange));
        when(marketDataProvider.fetchHistorical(eq(INSTRUMENT), any(), any())).thenReturn(List.of());

        service(0).ensureHistoricalData(INSTRUMENT, config);

        verify(marketDataProvider, times(1)).fetchHistorical(eq(INSTRUMENT), any(), any());
        verify(statusRegistry).historicalReady(eq("BTCUSDT"), any(Instant.class));
    }
}
