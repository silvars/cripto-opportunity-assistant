package com.btcassistant.market;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Garante o histórico necessário antes de iniciar o live stream, por instrumento habilitado
 * (SDD Fase 0 §2). O backfill roda em thread virtual para não bloquear o boot do contexto Spring.
 */
@Component
public class MarketStreamingStarter implements ApplicationRunner {

    private final MarketProperties marketProperties;
    private final MarketDataProvider marketDataProvider;
    private final CandleIngestionService candleIngestionService;
    private final HistoricalBackfillService historicalBackfillService;
    private final MarketStatusRegistry statusRegistry;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public MarketStreamingStarter(MarketProperties marketProperties,
                                  MarketDataProvider marketDataProvider,
                                  CandleIngestionService candleIngestionService,
                                  HistoricalBackfillService historicalBackfillService,
                                  MarketStatusRegistry statusRegistry) {
        this.marketProperties = marketProperties;
        this.marketDataProvider = marketDataProvider;
        this.candleIngestionService = candleIngestionService;
        this.historicalBackfillService = historicalBackfillService;
        this.statusRegistry = statusRegistry;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (MarketProperties.InstrumentConfig config : marketProperties.getInstruments()) {
            if (!config.isEnabled()) {
                continue;
            }
            Instrument instrument = config.toInstrument();
            executor.submit(() -> {
                statusRegistry.updatePhase(instrument.symbol(), MarketPhase.INITIALIZING);
                historicalBackfillService.ensureHistoricalData(instrument, config.getHistoricalBackfill());
                statusRegistry.updatePhase(instrument.symbol(), MarketPhase.LIVE);
                marketDataProvider.streamLive(instrument, candleIngestionService);
            });
        }
    }
}
