package com.btcassistant.risk;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Composição determinística de três sinais (volatilidade, proximidade de extremos, magnitude do
 * evento) em um risco 0-1, mapeado para LOW/MEDIUM/HIGH. UNKNOWN quando a volatilidade de 24h
 * (sinal mais importante) não está disponível — nunca "inventa" confiança (SDD §25/§26).
 */
@Component
public class SimpleRiskEngine implements RiskEngine {

    private final RiskProperties properties;

    public SimpleRiskEngine(RiskProperties properties) {
        this.properties = properties;
    }

    @Override
    public RiskAssessment assess(DetectedEvent event, FeatureSnapshot features) {
        BigDecimal volatility24h = features.values().get(FeatureNames.VOLATILITY_24H);
        if (volatility24h == null) {
            return new RiskAssessment(RiskLevel.UNKNOWN, List.of("insufficient_volatility_data"));
        }

        BigDecimal volatilityComponent = clamp01(volatility24h.divide(properties.getHighVolatilityScale(), MathContext.DECIMAL64));
        BigDecimal proximityComponent = proximityComponent(features);
        BigDecimal magnitudeComponent = clamp01(event.magnitude().abs().divide(properties.getMagnitudeScale(), MathContext.DECIMAL64));

        BigDecimal composite = volatilityComponent.multiply(properties.getVolatilityWeight())
                .add(proximityComponent.multiply(properties.getProximityWeight()))
                .add(magnitudeComponent.multiply(properties.getMagnitudeWeight()));

        List<String> reasons = new ArrayList<>();
        BigDecimal significantThreshold = properties.getSignificantComponentThreshold();
        if (volatilityComponent.compareTo(significantThreshold) >= 0) {
            reasons.add("high_volatility");
        }
        if (proximityComponent.compareTo(significantThreshold) >= 0) {
            reasons.add("near_recent_extreme");
        }
        if (magnitudeComponent.compareTo(significantThreshold) >= 0) {
            reasons.add("large_magnitude_move");
        }
        if (reasons.isEmpty()) {
            reasons.add("no_significant_risk_signals");
        }

        RiskLevel level;
        if (composite.compareTo(properties.getHighRiskThreshold()) >= 0) {
            level = RiskLevel.HIGH;
        } else if (composite.compareTo(properties.getMediumRiskThreshold()) >= 0) {
            level = RiskLevel.MEDIUM;
        } else {
            level = RiskLevel.LOW;
        }

        return new RiskAssessment(level, List.copyOf(reasons));
    }

    private BigDecimal proximityComponent(FeatureSnapshot features) {
        BigDecimal distanceHigh = features.values().get(FeatureNames.DISTANCE_RECENT_HIGH_1H);
        BigDecimal distanceLow = features.values().get(FeatureNames.DISTANCE_RECENT_LOW_1H);
        if (distanceHigh == null || distanceLow == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal nearHigh = clamp01(BigDecimal.ONE.subtract(
                distanceHigh.abs().divide(properties.getProximityScale(), MathContext.DECIMAL64)));
        BigDecimal nearLow = clamp01(BigDecimal.ONE.subtract(
                distanceLow.abs().divide(properties.getProximityScale(), MathContext.DECIMAL64)));
        return nearHigh.max(nearLow);
    }

    private static BigDecimal clamp01(BigDecimal value) {
        return value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }
}
