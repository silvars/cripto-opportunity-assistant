package com.btcassistant.event;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

/**
 * Rompimento de máxima: a janela de 24h inclui sempre o candle atual (SDD Fase 1 §4 — RollingWindow
 * inclui o candle de refer\u00eancia), ent\u00e3o {@code distance_recent_high_24h == 0} s\u00f3 por si s\u00f3 n\u00e3o distingue
 * "acabou de romper" de "j\u00e1 estava na m\u00e1xima h\u00e1 v\u00e1rios candles". BREAKOUT dispara apenas na transi\u00e7\u00e3o:
 * snapshot anterior abaixo da m\u00e1xima, snapshot atual na m\u00e1xima (SDD §20).
 */
@Component
public class BreakoutRule implements EventRule {

    private final EventProperties properties;

    public BreakoutRule(EventProperties properties) {
        this.properties = properties;
    }

    @Override
    public String eventType() {
        return EventTypes.BREAKOUT;
    }

    @Override
    public String engineVersion() {
        return properties.getEngineVersion();
    }

    @Override
    public Optional<DetectedEvent> evaluate(BigDecimal price, FeatureSnapshot features, FeatureHistory recentHistory) {
        BigDecimal currentDistance = features.values().get(FeatureNames.DISTANCE_RECENT_HIGH_24H);
        Optional<FeatureSnapshot> previous = recentHistory.previous();
        if (currentDistance == null || currentDistance.signum() != 0 || previous.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal previousDistance = previous.get().values().get(FeatureNames.DISTANCE_RECENT_HIGH_24H);
        if (previousDistance == null || previousDistance.signum() == 0) {
            return Optional.empty();
        }

        Duration duration = Duration.between(previous.get().candleOpenTime(), features.candleOpenTime());
        return Optional.of(new DetectedEvent(features.symbol(), eventType(), features.candleOpenTime(), price,
                features.values(), previousDistance.abs(), duration, engineVersion()));
    }
}
