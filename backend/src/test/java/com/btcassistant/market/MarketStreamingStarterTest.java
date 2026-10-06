package com.btcassistant.market;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketStreamingStarterTest {

    @Mock
    private MarketProperties marketProperties;
    @Mock
    private MarketDataProvider marketDataProvider;
    @Mock
    private CandleIngestionService candleIngestionService;
    @Mock
    private HistoricalBackfillService historicalBackfillService;
    @Mock
    private MarketStatusRegistry statusRegistry;

    private MarketStreamingStarter starter;

    @BeforeEach
    void setUp() {
        starter = new MarketStreamingStarter(marketProperties, marketDataProvider, candleIngestionService,
                historicalBackfillService, statusRegistry);
    }

    private static MarketProperties.InstrumentConfig configFor(String symbol, boolean enabled) {
        MarketProperties.InstrumentConfig config = new MarketProperties.InstrumentConfig();
        config.setSymbol(symbol);
        config.setBaseAsset("BTC");
        config.setQuoteAsset("USDT");
        config.setExchange("BINANCE");
        config.setInterval("1m");
        config.setEnabled(enabled);
        return config;
    }

    @Test
    void backfillsThenStreamsOnlyEnabledInstruments() {
        MarketProperties.InstrumentConfig enabled = configFor("BTCUSDT", true);
        MarketProperties.InstrumentConfig disabled = configFor("ETHUSDT", false);
        when(marketProperties.getInstruments()).thenReturn(List.of(enabled, disabled));

        starter.run(null);

        // o trabalho roda em thread virtual (não bloqueia o boot) — aguarda a conclusão.
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
                verify(marketDataProvider).streamLive(eq(enabled.toInstrument()), eq(candleIngestionService)));

        verify(marketDataProvider, never()).streamLive(eq(disabled.toInstrument()), any());
        verify(historicalBackfillService).ensureHistoricalData(eq(enabled.toInstrument()), eq(enabled.getHistoricalBackfill()));
    }
}
