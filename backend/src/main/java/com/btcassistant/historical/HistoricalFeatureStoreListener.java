package com.btcassistant.historical;

import com.btcassistant.event.MarketEventPersistedEvent;
import com.btcassistant.feature.FeatureProperties;
import com.btcassistant.feature.FeatureSnapshot;
import com.btcassistant.feature.MarketFeatureRepository;
import com.btcassistant.market.MarketProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Persiste as features normalizadas de TODO evento detectado — mesmo os que não viram
 * oportunidade — para que eventos futuros possam encontrá-los como candidatos (SDD Fase 4 §3/§6).
 */
@Component
public class HistoricalFeatureStoreListener {

    private static final Logger log = LoggerFactory.getLogger(HistoricalFeatureStoreListener.class);

    private final MarketProperties marketProperties;
    private final MarketFeatureRepository featureRepository;
    private final FeatureProperties featureProperties;
    private final HistoricalFeatureNormalizer normalizer;
    private final HistoricalFeatureStoreRepository repository;
    private final HistoricalProperties historicalProperties;

    public HistoricalFeatureStoreListener(MarketProperties marketProperties,
                                          MarketFeatureRepository featureRepository,
                                          FeatureProperties featureProperties,
                                          HistoricalFeatureNormalizer normalizer,
                                          HistoricalFeatureStoreRepository repository,
                                          HistoricalProperties historicalProperties) {
        this.marketProperties = marketProperties;
        this.featureRepository = featureRepository;
        this.featureProperties = featureProperties;
        this.normalizer = normalizer;
        this.repository = repository;
        this.historicalProperties = historicalProperties;
    }

    @EventListener
    public void onMarketEventPersisted(MarketEventPersistedEvent persistedEvent) {
        var event = persistedEvent.event();
        var instrument = marketProperties.findEnabledBySymbol(event.symbol());
        if (instrument == null) {
            return;
        }

        Optional<FeatureSnapshot> features = featureRepository
                .findBySymbolAndIntervalAndCandleOpenTimeAndFeatureVersion(
                        event.symbol(), instrument.interval(), event.eventTime(), featureProperties.getVersion())
                .map(entity -> entity.toSnapshot());
        if (features.isEmpty()) {
            log.warn("historical_feature_store_skipped symbol={} eventId={} reason=missing-feature-snapshot",
                    event.symbol(), persistedEvent.eventId());
            return;
        }

        Map<String, BigDecimal> normalized = normalizer.normalize(event, features.get());
        repository.save(HistoricalFeatureStoreEntity.create(event.symbol(), persistedEvent.eventId(), event.eventType(),
                event.eventTime(), normalized, historicalProperties.getVersion()));
    }
}
