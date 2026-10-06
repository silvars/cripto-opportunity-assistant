package com.btcassistant.market;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Mantém e publica o estado de ingestão por instrumento (SDD Fase 0 §16). */
@Component
public class MarketStatusRegistry {

    private final Map<String, MutableStatus> statuses = new ConcurrentHashMap<>();
    private final CandlePublisher candlePublisher;
    private final CandleRepository candleRepository;

    public MarketStatusRegistry(CandlePublisher candlePublisher, CandleRepository candleRepository) {
        this.candlePublisher = candlePublisher;
        this.candleRepository = candleRepository;
    }

    public void updatePhase(String symbol, MarketPhase phase) {
        state(symbol).phase = phase;
        publish(symbol);
    }

    public void websocketConnected(String symbol, boolean connected) {
        state(symbol).websocketConnected = connected;
        publish(symbol);
    }

    public void historicalReady(String symbol, Instant requiredHistoryStart) {
        MutableStatus status = state(symbol);
        status.historicalDataReady = true;
        status.requiredHistoryStart = requiredHistoryStart;
        status.phase = MarketPhase.HISTORICAL_DATA_READY;
        publish(symbol);
    }

    public void candlePersisted(String symbol, String interval, Instant openTime) {
        MutableStatus status = state(symbol);
        status.candleCount = candleRepository.countBySymbolAndInterval(symbol, interval);
        if (status.earliestCandle == null || openTime.isBefore(status.earliestCandle)) {
            status.earliestCandle = openTime;
        }
        if (status.latestCandle == null || openTime.isAfter(status.latestCandle)) {
            status.latestCandle = openTime;
        }
        publish(symbol);
    }

    /**
     * Atualiza contagem/limites a partir de agregados do banco — uma query de contagem + duas de
     * extremos, em vez de uma atualização por candle. Usar após operações em lote (backfill/gap
     * recovery) para não gerar milhares de queries/publicações WebSocket durante o backfill inicial.
     */
    public void refreshCounts(String symbol, String interval) {
        MutableStatus status = state(symbol);
        status.candleCount = candleRepository.countBySymbolAndInterval(symbol, interval);
        candleRepository.findTopBySymbolAndIntervalOrderByOpenTimeAsc(symbol, interval)
                .ifPresent(entity -> status.earliestCandle = entity.getOpenTime());
        candleRepository.findTopBySymbolAndIntervalOrderByOpenTimeDesc(symbol, interval)
                .ifPresent(entity -> status.latestCandle = entity.getOpenTime());
        publish(symbol);
    }

    public MarketStatus snapshot(String symbol) {
        MutableStatus status = state(symbol);
        return new MarketStatus(
                symbol,
                status.phase,
                status.websocketConnected,
                status.historicalDataReady,
                status.candleCount,
                status.earliestCandle,
                status.latestCandle,
                status.requiredHistoryStart,
                Instant.now()
        );
    }

    private void publish(String symbol) {
        candlePublisher.publishStatus(snapshot(symbol));
    }

    private MutableStatus state(String symbol) {
        return statuses.computeIfAbsent(symbol, s -> new MutableStatus());
    }

    private static final class MutableStatus {
        volatile MarketPhase phase = MarketPhase.INITIALIZING;
        volatile boolean websocketConnected;
        volatile boolean historicalDataReady;
        volatile long candleCount;
        volatile Instant earliestCandle;
        volatile Instant latestCandle;
        volatile Instant requiredHistoryStart;
    }
}
