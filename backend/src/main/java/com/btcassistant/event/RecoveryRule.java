package com.btcassistant.event;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

/**
 * Recuperação: o snapshot anterior estava próximo da mínima recente (aproximação de mínima) e o
 * atual já mostra retorno positivo relevante — o preço "saiu" da zona de mínima (SDD §20).
 */
@Component
public class RecoveryRule implements EventRule {

    private final EventProperties properties;

    public RecoveryRule(EventProperties properties) {
        this.properties = properties;
    }

    @Override
    public String eventType() {
        return EventTypes.RECOVERY;
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

        BigDecimal previousLowDistance = previous.get().values().get(FeatureNames.DISTANCE_RECENT_LOW_1H);
        BigDecimal currentReturn = features.values().get(FeatureNames.RETURN_15M);
        if (previousLowDistance == null || currentReturn == null) {
            return Optional.empty();
        }

        boolean wasNearLow = previousLowDistance.compareTo(properties.getRules().getLocalLowApproachDistance()) <= 0;
        boolean bouncedBack = currentReturn.compareTo(properties.getRules().getRecoveryThreshold()) >= 0;
        if (!wasNearLow || !bouncedBack) {
            return Optional.empty();
        }

        Duration duration = Duration.between(previous.get().candleOpenTime(), features.candleOpenTime());
        return Optional.of(new DetectedEvent(features.symbol(), eventType(), features.candleOpenTime(), price,
                features.values(), currentReturn, duration, engineVersion()));
    }
}
