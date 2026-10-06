package com.btcassistant.market;

import java.time.Instant;

/** Snapshot do estado de ingestão exposto via API/WebSocket para o dashboard (SDD Fase 0 §11/§16). */
public record MarketStatus(
    String symbol,
    MarketPhase phase,
    boolean websocketConnected,
    boolean historicalDataReady,
    long candleCount,
    Instant earliestCandle,
    Instant latestCandle,
    Instant requiredHistoryStart,
    Instant updatedAt
) {
}
