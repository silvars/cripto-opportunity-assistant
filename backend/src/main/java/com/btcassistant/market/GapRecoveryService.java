package com.btcassistant.market;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/** Recuperação de gaps via REST (SDD §14/§20) — o live stream sozinho nunca é fonte suficiente. */
@Service
public class GapRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(GapRecoveryService.class);

    private final MarketDataProvider marketDataProvider;
    private final CandlePersister candlePersister;
    private final MarketProperties marketProperties;
    private final MarketStatusRegistry statusRegistry;

    public GapRecoveryService(MarketDataProvider marketDataProvider,
                              CandlePersister candlePersister,
                              MarketProperties marketProperties,
                              MarketStatusRegistry statusRegistry) {
        this.marketDataProvider = marketDataProvider;
        this.candlePersister = candlePersister;
        this.marketProperties = marketProperties;
        this.statusRegistry = statusRegistry;
    }

    public void recover(String symbol, String interval, Instant from, Instant to) {
        Instrument instrument = marketProperties.findEnabled(symbol, interval);
        if (instrument == null) {
            return;
        }

        log.info("gap_recovery_started symbol={} interval={} from={} to={}", symbol, interval, from, to);
        List<Candle> missing = marketDataProvider.fetchHistorical(instrument, from, to);
        int persisted = 0;
        for (Candle candle : missing) {
            if (candlePersister.persist(candle)) {
                persisted++;
            }
        }
        statusRegistry.refreshCounts(symbol, interval);
        log.info("gap_recovery_completed symbol={} interval={} fetched={} persisted={}", symbol, interval, missing.size(), persisted);
    }
}
