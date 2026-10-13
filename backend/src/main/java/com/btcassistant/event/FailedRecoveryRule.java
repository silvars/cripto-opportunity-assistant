package com.btcassistant.event;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

/**
 * Recuperação falha: o snapshot anterior mostrava momentum positivo (tentativa de recuperação),
 * mas o atual reverteu para baixo e ainda está perto da mínima recente — a tentativa não vingou (SDD §20).
 */
@Component
public class FailedRecoveryRule implements EventRule {

    private final EventProperties properties;

    public FailedRecoveryRule(EventProperties properties) {
        this.properties = properties;
    }

    @Override
    public String eventType() {
        return EventTypes.FAILED_RECOVERY;
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

        BigDecimal previousMomentum = previous.get().values().get(FeatureNames.MOMENTUM_15M);
        BigDecimal currentReturn = features.values().get(FeatureNames.RETURN_15M);
        BigDecimal currentLowDistance = features.values().get(FeatureNames.DISTANCE_RECENT_LOW_1H);
        if (previousMomentum == null || currentReturn == null || currentLowDistance == null) {
            return Optional.empty();
        }

        boolean wasRecovering = previousMomentum.signum() > 0;
        boolean reversedDown = currentReturn.compareTo(properties.getRules().getFailedRecoveryThreshold().negate()) <= 0;
        boolean stillNearLow = currentLowDistance.compareTo(properties.getRules().getLocalLowApproachDistance()) <= 0;
        if (!wasRecovering || !reversedDown || !stillNearLow) {
            return Optional.empty();
        }

        Duration duration = Duration.between(previous.get().candleOpenTime(), features.candleOpenTime());
        return Optional.of(new DetectedEvent(features.symbol(), eventType(), features.candleOpenTime(), price,
                features.values(), currentReturn.abs(), duration, engineVersion()));
    }
}
