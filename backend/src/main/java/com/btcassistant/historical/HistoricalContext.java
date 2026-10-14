package com.btcassistant.historical;

import java.util.List;
import java.util.Map;

/**
 * Contexto histórico produzido pelo {@link HistoricalAnalyzer} (SDD Fase 4 §4). Substitui o
 * placeholder {@code opportunity.HistoricalContext} da Fase 3 (interface vazia, sempre
 * {@code Optional.empty()}).
 */
public record HistoricalContext(
    int sampleSize,
    List<SimilarEvent> similarEvents,
    Map<String, OutcomeStats> outcomesByHorizon,
    String historicalVersion
) {

    /** @return instância vazia (amostra insuficiente) — nunca {@code null}, usada quando não há contexto confiável. */
    public static HistoricalContext empty(String historicalVersion) {
        return new HistoricalContext(0, List.of(), Map.of(), historicalVersion);
    }
}
