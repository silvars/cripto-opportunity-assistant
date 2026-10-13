package com.btcassistant.event;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.util.Optional;

/** Expansão de volatilidade: {@code volatility_15m / volatility_4h >= ratio} (curto prazo bem acima do longo prazo) (SDD §20). */
@Component
public class VolatilityExpansionRule implements EventRule {

    private final EventProperties properties;

    public VolatilityExpansionRule(EventProperties properties) {
        this.properties = properties;
    }

    @Override
    public String eventType() {
        return EventTypes.VOLATILITY_EXPANSION;
    }

    @Override
    public String engineVersion() {
        return properties.getEngineVersion();
    }

    @Override
    public Optional<DetectedEvent> evaluate(BigDecimal price, FeatureSnapshot features, FeatureHistory recentHistory) {
        BigDecimal shortTerm = features.values().get(FeatureNames.VOLATILITY_15M);
        BigDecimal longTerm = features.values().get(FeatureNames.VOLATILITY_4H);
        if (shortTerm == null || longTerm == null || longTerm.signum() == 0) {
            return Optional.empty();
        }

        BigDecimal ratio = shortTerm.divide(longTerm, MathContext.DECIMAL64);
        if (ratio.compareTo(properties.getRules().getVolatilityExpansionRatio()) < 0) {
            return Optional.empty();
        }

        return Optional.of(new DetectedEvent(features.symbol(), eventType(), features.candleOpenTime(), price,
                features.values(), ratio, Duration.ofMinutes(15), engineVersion()));
    }
}
