package com.btcassistant.opportunity;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.event.EventTypes;
import com.btcassistant.feature.FeatureSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;
import java.util.Set;

/**
 * Normaliza a magnitude do evento (unidade varia por tipo — percentual, z-score, razão) em um
 * valor [0,1] comparável, por "família" de evento (SDD Fase 3 §11). Único componente que nunca
 * é UNKNOWN — todo {@link DetectedEvent} sempre carrega uma magnitude.
 */
@Component
public class EventStrengthComponent implements ScoreComponentCalculator {

    private static final Set<String> SHOCK_EVENTS = Set.of(
            EventTypes.RAPID_DROP, EventTypes.RAPID_RISE, EventTypes.RECOVERY,
            EventTypes.FAILED_RECOVERY, EventTypes.FAILED_BREAKOUT, EventTypes.BREAKOUT, EventTypes.BREAKDOWN);
    private static final Set<String> PROXIMITY_EVENTS = Set.of(
            EventTypes.LOCAL_LOW_APPROACH, EventTypes.LOCAL_HIGH_APPROACH);

    private final OpportunityProperties properties;

    public EventStrengthComponent(OpportunityProperties properties) {
        this.properties = properties;
    }

    @Override
    public String componentName() {
        return "eventStrength";
    }

    @Override
    public BigDecimal weight(OpportunityProperties.Scoring scoring) {
        return scoring.getEventStrength();
    }

    @Override
    public Optional<BigDecimal> calculate(Direction direction, DetectedEvent event, FeatureSnapshot features) {
        OpportunityProperties.EventStrengthScales scales = properties.getEventStrengthScales();
        BigDecimal magnitude = event.magnitude().abs();
        String eventType = event.eventType();

        BigDecimal normalized;
        if (SHOCK_EVENTS.contains(eventType)) {
            normalized = clamp01(magnitude.divide(scales.getShockMagnitude(), MathContext.DECIMAL64));
        } else if (PROXIMITY_EVENTS.contains(eventType)) {
            // quanto MENOR a distância até o extremo, MAIS forte o sinal (inverte a normalização)
            normalized = clamp01(BigDecimal.ONE.subtract(magnitude.divide(scales.getProximityMagnitude(), MathContext.DECIMAL64)));
        } else if (EventTypes.VOLUME_SPIKE.equals(eventType)) {
            normalized = clamp01(magnitude.divide(scales.getVolumeSpikeMagnitude(), MathContext.DECIMAL64));
        } else if (EventTypes.VOLATILITY_EXPANSION.equals(eventType)) {
            normalized = clamp01(magnitude.divide(scales.getVolatilityExpansionMagnitude(), MathContext.DECIMAL64));
        } else if (EventTypes.VOLATILITY_CONTRACTION.equals(eventType)) {
            // ratio menor = contração mais forte
            normalized = clamp01(BigDecimal.ONE.subtract(magnitude.divide(scales.getVolatilityContractionMagnitude(), MathContext.DECIMAL64)));
        } else {
            normalized = clamp01(magnitude.divide(scales.getShockMagnitude(), MathContext.DECIMAL64));
        }

        return Optional.of(normalized);
    }

    private static BigDecimal clamp01(BigDecimal value) {
        return value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }
}
