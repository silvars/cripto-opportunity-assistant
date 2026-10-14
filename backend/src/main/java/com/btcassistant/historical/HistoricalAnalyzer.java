package com.btcassistant.historical;

import com.btcassistant.event.DetectedEvent;

import java.time.Instant;

/** Encontra situações historicamente semelhantes ao evento atual (SDD §27). */
public interface HistoricalAnalyzer {

    /**
     * @param asOf normalmente {@code event.eventTime()} — nenhum candidato com
     *             {@code eventTime >= asOf} é considerado (SDD §31, estrito).
     */
    HistoricalContext findSimilar(DetectedEvent event, Instant asOf);
}
