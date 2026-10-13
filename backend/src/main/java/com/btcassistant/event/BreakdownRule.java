package com.btcassistant.event;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

/** Rompimento de mínima — análogo a {@link BreakoutRule}, mas para a mínima de 24h (SDD §20). */
@Component
public class BreakdownRule implements EventRule {

    private final EventProperties properties;

    public BreakdownRule(EventProperties properties) {
        this.properties = properties;
    }

    @Override
    public String eventType() {
        return EventTypes.BREAKDOWN;
    }

    @Override
    public String engineVersion() {
        return properties.getEngineVersion();
    }

    @Override
    public Optional<DetectedEvent> evaluate(BigDecimal price, FeatureSnapshot features, FeatureHistory recentHistory) {
        BigDecimal currentDistance = features.values().get(FeatureNames.DISTANCE_RECENT_LOW_24H);
        Optional<FeatureSnapshot> previous = recentHistory.previous();
        if (currentDistance == null || currentDistance.signum() != 0 || previous.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal previousDistance = previous.get().values().get(FeatureNames.DISTANCE_RECENT_LOW_24H);
        if (previousDistance == null || previousDistance.signum() == 0) {
            return Optional.empty();
        }

        Duration duration = Duration.between(previous.get().candleOpenTime(), features.candleOpenTime());
        return Optional.of(new DetectedEvent(features.symbol(), eventType(), features.candleOpenTime(), price,
                features.values(), previousDistance.abs(), duration, engineVersion()));
    }
}
