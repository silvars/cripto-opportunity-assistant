package com.btcassistant.market;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Inicia o live stream para cada instrumento habilitado (SDD §7). */
@Component
public class MarketStreamingStarter implements ApplicationRunner {

    private final MarketProperties marketProperties;
    private final MarketDataProvider marketDataProvider;
    private final CandleIngestionService candleIngestionService;

    public MarketStreamingStarter(MarketProperties marketProperties,
                                  MarketDataProvider marketDataProvider,
                                  CandleIngestionService candleIngestionService) {
        this.marketProperties = marketProperties;
        this.marketDataProvider = marketDataProvider;
        this.candleIngestionService = candleIngestionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Instrument instrument : marketProperties.enabledInstruments()) {
            marketDataProvider.streamLive(instrument, candleIngestionService);
        }
    }
}
