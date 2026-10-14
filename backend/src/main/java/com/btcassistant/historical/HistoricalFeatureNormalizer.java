package com.btcassistant.historical;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Converte um evento+features no mapa de seis dimensões comparáveis (SDD §27). Como a busca de
 * similaridade sempre filtra por {@code event_type} igual, não é preciso normalizar por "família"
 * de evento (diferente de {@code opportunity.EventStrengthComponent}) — os valores já nascem na
 * mesma unidade dentro de um mesmo tipo. A única correção necessária é {@code momentum}: a feature
 * {@code momentum_15m} é um delta de preço absoluto (SDD Fase 1 §11), o que o tornaria enviesado
 * por nível de preço ao comparar eventos de meses diferentes — é dividido pelo preço do evento
 * para virar um percentual, igual às demais dimensões (ver docs/phases/FASE-4-historical-analyzer.md §11).
 */
@Component
public class HistoricalFeatureNormalizer {

    public Map<String, BigDecimal> normalize(DetectedEvent event, FeatureSnapshot features) {
        Map<String, BigDecimal> result = new HashMap<>();
        Map<String, BigDecimal> values = features.values();

        result.put(HistoricalFeatureNames.MAGNITUDE, event.magnitude().abs());
        result.put(HistoricalFeatureNames.DURATION, durationToMinutes(event.duration()));

        BigDecimal volumeRatio = values.get(FeatureNames.VOLUME_RATIO);
        if (volumeRatio != null) {
            result.put(HistoricalFeatureNames.VOLUME_RATIO, volumeRatio);
        }

        BigDecimal volatility = values.get(FeatureNames.VOLATILITY_15M);
        if (volatility != null) {
            result.put(HistoricalFeatureNames.VOLATILITY, volatility);
        }

        BigDecimal momentum = values.get(FeatureNames.MOMENTUM_15M);
        if (momentum != null && event.price().signum() != 0) {
            result.put(HistoricalFeatureNames.MOMENTUM, momentum.divide(event.price(), MathContext.DECIMAL64));
        }

        BigDecimal distanceToExtreme = distanceToExtreme(values);
        if (distanceToExtreme != null) {
            result.put(HistoricalFeatureNames.DISTANCE_TO_EXTREME, distanceToExtreme);
        }

        return result;
    }

    private static BigDecimal distanceToExtreme(Map<String, BigDecimal> values) {
        BigDecimal distanceHigh = values.get(FeatureNames.DISTANCE_RECENT_HIGH_1H);
        BigDecimal distanceLow = values.get(FeatureNames.DISTANCE_RECENT_LOW_1H);
        if (distanceHigh == null && distanceLow == null) {
            return null;
        }
        if (distanceHigh == null) {
            return distanceLow.abs();
        }
        if (distanceLow == null) {
            return distanceHigh.abs();
        }
        return distanceHigh.abs().min(distanceLow.abs());
    }

    private static BigDecimal durationToMinutes(Duration duration) {
        return BigDecimal.valueOf(duration.toSeconds()).divide(BigDecimal.valueOf(60), MathContext.DECIMAL64);
    }
}
