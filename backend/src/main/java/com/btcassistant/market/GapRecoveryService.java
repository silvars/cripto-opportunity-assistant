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
    private final CandleRepository candleRepository;
    private final MarketProperties marketProperties;

    public GapRecoveryService(MarketDataProvider marketDataProvider,
                              CandleRepository candleRepository,
                              MarketProperties marketProperties) {
        this.marketDataProvider = marketDataProvider;
        this.candleRepository = candleRepository;
        this.marketProperties = marketProperties;
    }

    public void recover(String symbol, String interval, Instant from, Instant to) {
        Instrument instrument = marketProperties.findEnabled(symbol, interval);
        if (instrument == null) {
            return;
        }

        log.info("backfill_started symbol={} interval={} from={} to={}", symbol, interval, from, to);
        List<Candle> missing = marketDataProvider.fetchHistorical(instrument, from, to);
        int persisted = 0;
        for (Candle candle : missing) {
            CandleValidator.ValidationResult result = CandleValidator.validate(candle);
            if (!result.valid()) {
                log.warn("candle_rejected symbol={} openTime={} reason={}", candle.symbol(), candle.openTime(), result.reason());
                continue;
            }
            boolean alreadyExists = candleRepository
                    .findBySymbolAndIntervalAndOpenTime(candle.symbol(), candle.interval(), candle.openTime())
                    .isPresent();
            if (!alreadyExists) {
                candleRepository.save(CandleEntity.from(candle));
                persisted++;
            }
        }
        log.info("backfill_completed symbol={} interval={} fetched={} persisted={}", symbol, interval, missing.size(), persisted);
    }
}
