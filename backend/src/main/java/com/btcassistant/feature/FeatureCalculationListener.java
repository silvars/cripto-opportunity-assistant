package com.btcassistant.feature;

import com.btcassistant.market.CandlePersistedEvent;
import com.btcassistant.market.Instrument;
import com.btcassistant.market.MarketProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Reage a candles novos da ingestão ao vivo — nunca a backfill/gap recovery em lote (SDD Fase 1 §6/§7). */
@Component
public class FeatureCalculationListener {

    private static final Logger log = LoggerFactory.getLogger(FeatureCalculationListener.class);

    private final MarketProperties marketProperties;
    private final FeatureCalculator featureCalculator;
    private final FeaturePersister featurePersister;
    private final ApplicationEventPublisher eventPublisher;

    public FeatureCalculationListener(MarketProperties marketProperties,
                                      FeatureCalculator featureCalculator,
                                      FeaturePersister featurePersister,
                                      ApplicationEventPublisher eventPublisher) {
        this.marketProperties = marketProperties;
        this.featureCalculator = featureCalculator;
        this.featurePersister = featurePersister;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    public void onCandlePersisted(CandlePersistedEvent event) {
        Instrument instrument = marketProperties.findEnabled(event.symbol(), event.interval());
        if (instrument == null) {
            return;
        }

        FeatureSnapshot snapshot = featureCalculator.calculate(instrument, event.openTime());
        if (snapshot == null) {
            log.warn("feature_calculation_failed symbol={} openTime={}", event.symbol(), event.openTime());
            return;
        }

        if (featurePersister.persist(snapshot)) {
            eventPublisher.publishEvent(new FeatureCalculatedEvent(
                    snapshot.symbol(), snapshot.interval(), snapshot.candleOpenTime(), snapshot.featureVersion()));
        }
    }
}
