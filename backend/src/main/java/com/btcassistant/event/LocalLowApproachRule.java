package com.btcassistant.event;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

/** Aproximação da mínima recente: {@code distance_recent_low_1h <= threshold} (preço perto da mínima, por cima) (SDD §20). */
@Component
public class LocalLowApproachRule implements EventRule {

    private final EventProperties properties;

    public LocalLowApproachRule(EventProperties properties) {
        this.properties = properties;
    }

    @Override
    public String eventType() {
        return EventTypes.LOCAL_LOW_APPROACH;
    }

    @Override
    public String engineVersion() {
        return properties.getEngineVersion();
    }

    @Override
    public Optional<DetectedEvent> evaluate(BigDecimal price, FeatureSnapshot features, FeatureHistory recentHistory) {
        BigDecimal distance = features.values().get(FeatureNames.DISTANCE_RECENT_LOW_1H);
        if (distance == null || distance.compareTo(properties.getRules().getLocalLowApproachDistance()) > 0) {
            return Optional.empty();
        }

        return Optional.of(new DetectedEvent(features.symbol(), eventType(), features.candleOpenTime(), price,
                features.values(), distance, Duration.ofHours(1), engineVersion()));
    }
}
