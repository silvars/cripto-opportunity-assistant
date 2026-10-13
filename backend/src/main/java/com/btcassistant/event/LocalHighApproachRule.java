package com.btcassistant.event;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

/** Aproximação da máxima recente: {@code distance_recent_high_1h >= -threshold} (preço perto da máxima, por baixo) (SDD §20). */
@Component
public class LocalHighApproachRule implements EventRule {

    private final EventProperties properties;

    public LocalHighApproachRule(EventProperties properties) {
        this.properties = properties;
    }

    @Override
    public String eventType() {
        return EventTypes.LOCAL_HIGH_APPROACH;
    }

    @Override
    public String engineVersion() {
        return properties.getEngineVersion();
    }

    @Override
    public Optional<DetectedEvent> evaluate(BigDecimal price, FeatureSnapshot features, FeatureHistory recentHistory) {
        BigDecimal distance = features.values().get(FeatureNames.DISTANCE_RECENT_HIGH_1H);
        BigDecimal threshold = properties.getRules().getLocalHighApproachDistance().negate();
        if (distance == null || distance.compareTo(threshold) < 0) {
            return Optional.empty();
        }

        return Optional.of(new DetectedEvent(features.symbol(), eventType(), features.candleOpenTime(), price,
                features.values(), distance, Duration.ofHours(1), engineVersion()));
    }
}
