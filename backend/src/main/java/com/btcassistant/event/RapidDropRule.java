package com.btcassistant.event;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

/** Queda rápida: {@code return_15m <= threshold} (threshold negativo, ex. -2%) (SDD §20). */
@Component
public class RapidDropRule implements EventRule {

    private final EventProperties properties;

    public RapidDropRule(EventProperties properties) {
        this.properties = properties;
    }

    @Override
    public String eventType() {
        return EventTypes.RAPID_DROP;
    }

    @Override
    public String engineVersion() {
        return properties.getEngineVersion();
    }

    @Override
    public Optional<DetectedEvent> evaluate(BigDecimal price, FeatureSnapshot features, FeatureHistory recentHistory) {
        BigDecimal return15m = features.values().get(FeatureNames.RETURN_15M);
        if (return15m == null || return15m.compareTo(properties.getRules().getRapidDropThreshold()) > 0) {
            return Optional.empty();
        }

        return Optional.of(new DetectedEvent(features.symbol(), eventType(), features.candleOpenTime(), price,
                features.values(), return15m.abs(), Duration.ofMinutes(15), engineVersion()));
    }
}
