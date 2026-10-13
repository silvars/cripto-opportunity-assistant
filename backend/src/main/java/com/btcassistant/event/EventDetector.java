package com.btcassistant.event;

import com.btcassistant.feature.FeatureCalculatedEvent;
import com.btcassistant.feature.FeatureSnapshot;
import com.btcassistant.feature.MarketFeatureEntity;
import com.btcassistant.feature.MarketFeatureRepository;
import com.btcassistant.market.CandleEntity;
import com.btcassistant.market.CandleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Orquestra todas as {@link EventRule} a cada novo {@link FeatureSnapshot} (SDD Fase 2 §4/§6). */
@Component
public class EventDetector {

    private static final Logger log = LoggerFactory.getLogger(EventDetector.class);

    private final MarketFeatureRepository featureRepository;
    private final CandleRepository candleRepository;
    private final List<EventRule> rules;
    private final EventPersister eventPersister;

    public EventDetector(MarketFeatureRepository featureRepository,
                         CandleRepository candleRepository,
                         List<EventRule> rules,
                         EventPersister eventPersister) {
        this.featureRepository = featureRepository;
        this.candleRepository = candleRepository;
        this.rules = rules;
        this.eventPersister = eventPersister;
    }

    @EventListener
    public void onFeatureCalculated(FeatureCalculatedEvent event) {
        Optional<MarketFeatureEntity> currentEntity = featureRepository
                .findBySymbolAndIntervalAndCandleOpenTimeAndFeatureVersion(
                        event.symbol(), event.interval(), event.candleOpenTime(), event.featureVersion());
        Optional<CandleEntity> candle = candleRepository
                .findBySymbolAndIntervalAndOpenTime(event.symbol(), event.interval(), event.candleOpenTime());
        if (currentEntity.isEmpty() || candle.isEmpty()) {
            log.warn("event_detection_skipped symbol={} openTime={} reason=missing-snapshot-or-candle",
                    event.symbol(), event.candleOpenTime());
            return;
        }

        FeatureSnapshot current = currentEntity.get().toSnapshot();
        BigDecimal price = candle.get().getClose();
        FeatureHistory history = loadHistory(event);

        for (EventRule rule : rules) {
            rule.evaluate(price, current, history).ifPresent(eventPersister::persist);
        }
    }

    private FeatureHistory loadHistory(FeatureCalculatedEvent event) {
        List<MarketFeatureEntity> recentDescending = featureRepository
                .findTop5BySymbolAndIntervalAndFeatureVersionAndCandleOpenTimeLessThanOrderByCandleOpenTimeDesc(
                        event.symbol(), event.interval(), event.featureVersion(), event.candleOpenTime());
        List<FeatureSnapshot> ascending = recentDescending.stream()
                .sorted(Comparator.comparing(MarketFeatureEntity::getCandleOpenTime))
                .map(MarketFeatureEntity::toSnapshot)
                .toList();
        return new FeatureHistory(ascending);
    }
}
