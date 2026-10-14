package com.btcassistant.historical;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Map;

/**
 * Distância ponderada entre dois vetores de features normalizadas (SDD §29). Dimensões ausentes
 * em qualquer um dos dois lados são excluídas do cálculo (e o peso é renormalizado sobre as
 * dimensões efetivamente disponíveis para aquele par) — nunca tratadas como zero.
 */
@Component
public class SimilarityCalculator {

    private static final List<String> WEIGHTED_DIMENSIONS = List.of(
            HistoricalFeatureNames.MAGNITUDE, HistoricalFeatureNames.DURATION, HistoricalFeatureNames.VOLUME_RATIO,
            HistoricalFeatureNames.VOLATILITY, HistoricalFeatureNames.MOMENTUM);

    public BigDecimal distance(Map<String, BigDecimal> a, Map<String, BigDecimal> b, HistoricalProperties.Weights weights) {
        BigDecimal weightedDiffSum = BigDecimal.ZERO;
        BigDecimal availableWeight = BigDecimal.ZERO;

        for (String dimension : WEIGHTED_DIMENSIONS) {
            BigDecimal valueA = a.get(dimension);
            BigDecimal valueB = b.get(dimension);
            if (valueA == null || valueB == null) {
                continue;
            }
            BigDecimal weight = weightOf(dimension, weights);
            weightedDiffSum = weightedDiffSum.add(weight.multiply(valueA.subtract(valueB).abs()));
            availableWeight = availableWeight.add(weight);
        }

        if (availableWeight.signum() == 0) {
            // nenhuma dimensão em comum — distância máxima (menos similar possível)
            return BigDecimal.TEN;
        }

        return weightedDiffSum.divide(availableWeight, MathContext.DECIMAL64);
    }

    /** @return valor em (0,1], 1.0 = idêntico, decrescente conforme a distância cresce. */
    public BigDecimal similarityScore(BigDecimal distance) {
        return BigDecimal.ONE.divide(BigDecimal.ONE.add(distance), MathContext.DECIMAL64);
    }

    private static BigDecimal weightOf(String dimension, HistoricalProperties.Weights weights) {
        return switch (dimension) {
            case HistoricalFeatureNames.MAGNITUDE -> weights.getMagnitude();
            case HistoricalFeatureNames.DURATION -> weights.getDuration();
            case HistoricalFeatureNames.VOLUME_RATIO -> weights.getVolumeRatio();
            case HistoricalFeatureNames.VOLATILITY -> weights.getVolatility();
            case HistoricalFeatureNames.MOMENTUM -> weights.getMomentum();
            default -> throw new IllegalStateException("Dimensão sem peso configurado: " + dimension);
        };
    }
}
