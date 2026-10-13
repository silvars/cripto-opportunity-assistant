package com.btcassistant.event;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

/** Pico de volume: {@code volume_zscore >= threshold} (ex. 2.5 desvios padrão) (SDD §20). */
@Component
public class VolumeSpikeRule implements EventRule {

    private final EventProperties properties;

    public VolumeSpikeRule(EventProperties properties) {
        this.properties = properties;
    }

    @Override
    public String eventType() {
        return EventTypes.VOLUME_SPIKE;
    }

    @Override
    public String engineVersion() {
        return properties.getEngineVersion();
    }

    @Override
    public Optional<DetectedEvent> evaluate(BigDecimal price, FeatureSnapshot features, FeatureHistory recentHistory) {
        BigDecimal zscore = features.values().get(FeatureNames.VOLUME_ZSCORE);
        if (zscore == null || zscore.compareTo(properties.getRules().getVolumeSpikeZscore()) < 0) {
            return Optional.empty();
        }

        return Optional.of(new DetectedEvent(features.symbol(), eventType(), features.candleOpenTime(), price,
                features.values(), zscore, Duration.ofMinutes(1), engineVersion()));
    }
}
