package com.btcassistant.event;

import com.btcassistant.feature.FeatureSnapshot;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Regra isolada e configurável para um único tipo de evento (SDD Fase 2 §4). Assinatura ajustada
 * em relação ao rascunho original do SDD: recebe {@code price} explicitamente, já que
 * {@link FeatureSnapshot} não carrega o preço do candle (ver docs/phases/FASE-2-event-detector.md §11).
 */
public interface EventRule {

    String eventType();

    String engineVersion();

    Optional<DetectedEvent> evaluate(BigDecimal price, FeatureSnapshot features, FeatureHistory recentHistory);
}
