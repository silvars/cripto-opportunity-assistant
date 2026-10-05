package com.btcassistant.market;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Orquestra validação, dedupe, detecção de gap, persistência e publicação (SDD §16). */
@Service
public class CandleIngestionService implements CandleListener {

    private static final Logger log = LoggerFactory.getLogger(CandleIngestionService.class);

    private final CandleRepository candleRepository;
    private final CandlePublisher candlePublisher;
    private final GapRecoveryService gapRecoveryService;

    public CandleIngestionService(CandleRepository candleRepository,
                                  CandlePublisher candlePublisher,
                                  GapRecoveryService gapRecoveryService) {
        this.candleRepository = candleRepository;
        this.candlePublisher = candlePublisher;
        this.gapRecoveryService = gapRecoveryService;
    }

    @Override
    public void onCandle(Candle candle) {
        CandleValidator.ValidationResult result = CandleValidator.validate(candle);
        if (!result.valid()) {
            log.warn("candle_rejected symbol={} openTime={} reason={}", candle.symbol(), candle.openTime(), result.reason());
            return;
        }

        boolean alreadyExists = candleRepository
                .findBySymbolAndIntervalAndOpenTime(candle.symbol(), candle.interval(), candle.openTime())
                .isPresent();
        if (alreadyExists) {
            return;
        }

        detectGap(candle);

        candleRepository.save(CandleEntity.from(candle));
        log.info("candle_persisted symbol={} interval={} openTime={}", candle.symbol(), candle.interval(), candle.openTime());
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
