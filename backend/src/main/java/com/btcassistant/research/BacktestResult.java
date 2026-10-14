package com.btcassistant.research;

import java.time.Instant;
import java.util.Map;

/**
 * Resultado completo de um backtest (SDD §64/§68). {@code startedAt}/{@code finishedAt} são
 * metadados de execução (variam a cada corrida); {@code summaryByEventType} é a parte que precisa
 * ser byte-a-byte idêntica entre duas execuções com as mesmas versões (DoD Fase 7 §7).
 */
public record BacktestResult(
    BacktestConfig config,
    int candlesProcessed,
    int eventsDetected,
    int opportunitiesGenerated,
    Map<String, EventTypeSummary> summaryByEventType,
    Instant startedAt,
    Instant finishedAt
) {
}
