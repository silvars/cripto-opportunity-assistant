package com.btcassistant.research;

import java.util.Map;

/** Estatísticas agregadas de um {@code eventType} (ex. RAPID_DROP), por horizonte (SDD §66). */
public record EventTypeSummary(
    int occurrences,
    Map<String, HorizonOutcomeStats> statsByHorizon
) {
}
