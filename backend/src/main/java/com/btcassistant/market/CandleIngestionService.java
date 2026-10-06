package com.btcassistant.market;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/** Orquestra detecção de gap, persistência (via CandlePersister) e publicação (SDD §16). */
@Service
public class CandleIngestionService implements CandleListener {

    private static final Logger log = LoggerFactory.getLogger(CandleIngestionService.class);

    private final CandlePersister candlePersister;
    private final CandleRepository candleRepository;
    private final CandlePublisher candlePublisher;
    private final GapRecoveryService gapRecoveryService;
    private final MarketStatusRegistry statusRegistry;

    public CandleIngestionService(CandlePersister candlePersister,
                                  CandleRepository candleRepository,
                                  CandlePublisher candlePublisher,
                                  GapRecoveryService gapRecoveryService,
                                  MarketStatusRegistry statusRegistry) {
        this.candlePersister = candlePersister;
        this.candleRepository = candleRepository;
        this.candlePublisher = candlePublisher;
        this.gapRecoveryService = gapRecoveryService;
        this.statusRegistry = statusRegistry;
    }

    @Override
    public void onCandle(Candle candle) {
        detectGap(candle);

        if (!candlePersister.persist(candle)) {
            return;
        }

        statusRegistry.candlePersisted(candle.symbol(), candle.interval(), candle.openTime());
        candlePublisher.publish(candle);
    }

    private void detectGap(Candle candle) {
        Optional<CandleEntity> last = candleRepository
                .findTopBySymbolAndIntervalOrderByOpenTimeDesc(candle.symbol(), candle.interval());

        last.ifPresent(previous -> {
            Duration actualStep = Duration.between(previous.getOpenTime(), candle.openTime());
            Duration expectedStep = IntervalSupport.toDuration(candle.interval());
            if (actualStep.compareTo(expectedStep) > 0) {
                log.warn("gap_detected symbol={} interval={} from={} to={}",
                        candle.symbol(), candle.interval(), previous.getOpenTime(), candle.openTime());
                gapRecoveryService.recover(candle.symbol(), candle.interval(), previous.getOpenTime(), candle.openTime());
            }
        });
    }
}
