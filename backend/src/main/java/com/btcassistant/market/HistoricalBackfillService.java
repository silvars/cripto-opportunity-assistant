package com.btcassistant.market;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Orquestra o backfill histórico inicial no boot (SDD Fase 0 — Initial Historical Backfill).
 * Fluxo: CHECKING_HISTORY -&gt; (BACKFILLING -&lt;-&gt; RETRYING)* -&gt; HISTORICAL_DATA_READY.
 */
@Service
public class HistoricalBackfillService {

    private static final Logger log = LoggerFactory.getLogger(HistoricalBackfillService.class);

    private final MarketDataProvider marketDataProvider;
    private final HistoricalCoverageChecker coverageChecker;
    private final CandlePersister candlePersister;
    private final MarketStatusRegistry statusRegistry;
    private final int maxRetries;

    public HistoricalBackfillService(MarketDataProvider marketDataProvider,
                                     HistoricalCoverageChecker coverageChecker,
                                     CandlePersister candlePersister,
                                     MarketStatusRegistry statusRegistry,
                                     @Value("${market.backfill.max-retries:3}") int maxRetries) {
        this.marketDataProvider = marketDataProvider;
        this.coverageChecker = coverageChecker;
        this.candlePersister = candlePersister;
        this.statusRegistry = statusRegistry;
        this.maxRetries = maxRetries;
    }

    public void ensureHistoricalData(Instrument instrument, MarketProperties.HistoricalBackfillConfig config) {
        String symbol = instrument.symbol();
        statusRegistry.updatePhase(symbol, MarketPhase.CHECKING_HISTORY);

        if (config == null || !config.isEnabled()) {
            log.info("historical_coverage_checked symbol={} status=skipped", symbol);
            statusRegistry.historicalReady(symbol, null);
            return;
        }

        Instant now = Instant.now();
        Instant requiredStart = alignToBoundary(BackfillPeriod.resolveStart(config.getPeriod(), now), instrument.interval());
        Instant requiredEnd = alignToBoundary(now, instrument.interval());

        List<HistoricalRange> missingRanges = coverageChecker.findMissingRanges(symbol, instrument.interval(), requiredStart, requiredEnd);
        log.info("historical_coverage_checked symbol={} requiredStart={} requiredEnd={} missingRanges={}",
                symbol, requiredStart, requiredEnd, missingRanges.size());

        if (missingRanges.isEmpty()) {
            statusRegistry.historicalReady(symbol, requiredStart);
            log.info("historical_backfill_completed symbol={} status=already-complete", symbol);
            return;
        }

        statusRegistry.updatePhase(symbol, MarketPhase.BACKFILLING);
        log.info("historical_backfill_started symbol={} ranges={}", symbol, missingRanges.size());

        int totalFetched = 0;
        int totalPersisted = 0;
        for (HistoricalRange range : missingRanges) {
            BackfillOutcome outcome = fillRangeWithRetry(instrument, range);
            totalFetched += outcome.fetched();
            totalPersisted += outcome.persisted();
        }

        statusRegistry.historicalReady(symbol, requiredStart);
        log.info("historical_backfill_completed symbol={} fetched={} persisted={}", symbol, totalFetched, totalPersisted);
    }

    private BackfillOutcome fillRangeWithRetry(Instrument instrument, HistoricalRange range) {
        int fetched = 0;
        int persisted = 0;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            List<Candle> candles = marketDataProvider.fetchHistorical(instrument, range.from(), range.to());
            fetched += candles.size();
            for (Candle candle : candles) {
                if (candlePersister.persist(candle)) {
                    persisted++;
                }
            }
            log.info("historical_backfill_batch symbol={} from={} to={} fetched={} persisted={}",
                    instrument.symbol(), range.from(), range.to(), candles.size(), persisted);
            statusRegistry.refreshCounts(instrument.symbol(), instrument.interval());

            boolean complete = coverageChecker
                    .findMissingRanges(instrument.symbol(), instrument.interval(), range.from(), range.to())
                    .isEmpty();
            if (complete) {
                return new BackfillOutcome(fetched, persisted);
            }
            if (attempt == maxRetries) {
                log.warn("historical_backfill_incomplete symbol={} from={} to={} afterAttempts={}",
                        instrument.symbol(), range.from(), range.to(), attempt + 1);
                return new BackfillOutcome(fetched, persisted);
            }

            statusRegistry.updatePhase(instrument.symbol(), MarketPhase.RETRYING);
            log.warn("historical_backfill_retry symbol={} attempt={}", instrument.symbol(), attempt + 1);
            sleep(Duration.ofSeconds(Math.min(30, 1L << (attempt + 1))));
            statusRegistry.updatePhase(instrument.symbol(), MarketPhase.BACKFILLING);
        }
        return new BackfillOutcome(fetched, persisted);
    }

    private Instant alignToBoundary(Instant instant, String interval) {
        long stepMillis = IntervalSupport.toDuration(interval).toMillis();
        long boundary = (instant.toEpochMilli() / stepMillis) * stepMillis;
        return Instant.ofEpochMilli(boundary);
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    private record BackfillOutcome(int fetched, int persisted) {
    }
}
