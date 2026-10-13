package com.btcassistant.feature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Upsert por {@code (symbol, interval, candleOpenTime, featureVersion)} — nunca sobrescreve uma linha existente (SDD §19). */
@Component
public class FeaturePersister {

    private static final Logger log = LoggerFactory.getLogger(FeaturePersister.class);

    private final MarketFeatureRepository featureRepository;

    public FeaturePersister(MarketFeatureRepository featureRepository) {
        this.featureRepository = featureRepository;
    }

    /** @return true se persistido agora (novo); false se já existia uma linha para essa chave. */
    public boolean persist(FeatureSnapshot snapshot) {
        boolean alreadyExists = featureRepository.findBySymbolAndIntervalAndCandleOpenTimeAndFeatureVersion(
                snapshot.symbol(), snapshot.interval(), snapshot.candleOpenTime(), snapshot.featureVersion()).isPresent();
        if (alreadyExists) {
            return false;
        }

        featureRepository.save(MarketFeatureEntity.from(snapshot));
        log.info("feature_persisted symbol={} interval={} openTime={} featureVersion={}",
                snapshot.symbol(), snapshot.interval(), snapshot.candleOpenTime(), snapshot.featureVersion());
        return true;
    }
}
