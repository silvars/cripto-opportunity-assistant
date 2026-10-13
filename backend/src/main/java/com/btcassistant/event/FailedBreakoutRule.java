package com.btcassistant.event;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

/**
 * Rompimento falho: o snapshot anterior tinha acabado de romper a máxima de 24h, mas o atual já
 * recuou abaixo dela com retorno negativo relevante — o rompimento não se sustentou (SDD §20).
 */
@Component
public class FailedBreakoutRule implements EventRule {

    private final EventProperties properties;

    public FailedBreakoutRule(EventProperties properties) {
        this.properties = properties;
    }

    @Override
    public String eventType() {
        return EventTypes.FAILED_BREAKOUT;
    }

    @Override
    public String engineVersion() {
        return properties.getEngineVersion();
    }

    @Override
    public Optional<DetectedEvent> evaluate(BigDecimal price, FeatureSnapshot features, FeatureHistory recentHistory) {
        Optional<FeatureSnapshot> previous = recentHistory.previous();
        if (previous.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal previousDistance = previous.get().values().get(FeatureNames.DISTANCE_RECENT_HIGH_24H);
        BigDecimal currentDistance = features.values().get(FeatureNames.DISTANCE_RECENT_HIGH_24H);
        BigDecimal currentReturn = features.values().get(FeatureNames.RETURN_15M);
        if (previousDistance == null || currentDistance == null || currentReturn == null) {
            return Optional.empty();
        }

        boolean hadJustBrokenOut = previousDistance.signum() == 0;
        boolean fellBackBelowHigh = currentDistance.signum() < 0;
        boolean reversedDown = currentReturn.compareTo(properties.getRules().getFailedBreakoutThreshold().negate()) <= 0;
        if (!hadJustBrokenOut || !fellBackBelowHigh || !reversedDown) {
            return Optional.empty();
        }

        Duration duration = Duration.between(previous.get().candleOpenTime(), features.candleOpenTime());
        return Optional.of(new DetectedEvent(features.symbol(), eventType(), features.candleOpenTime(), price,
                features.values(), currentReturn.abs(), duration, engineVersion()));
    }
}
