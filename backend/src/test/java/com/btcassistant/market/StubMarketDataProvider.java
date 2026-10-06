package com.btcassistant.market;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dublê de teste do MarketDataProvider — nunca bate na Binance real. Gera candles sintéticos e
 * gapless para qualquer range pedido, permitindo validar o mecanismo de backfill/coverage contra
 * PostgreSQL real (Testcontainers) sem depender de rede externa.
 */
@Component
@Profile("test")
@Primary
public class StubMarketDataProvider implements MarketDataProvider {

    private final AtomicInteger fetchCalls = new AtomicInteger();

    @Override
    public void streamLive(Instrument instrument, CandleListener listener) {
        // no-op em testes — não conecta em nenhum WebSocket real.
    }

    @Override
    public List<Candle> fetchHistorical(Instrument instrument, Instant from, Instant to) {
        fetchCalls.incrementAndGet();
        List<Candle> candles = new ArrayList<>();
        Instant cursor = from;
        while (cursor.isBefore(to)) {
            candles.add(syntheticCandle(instrument, cursor));
            cursor = cursor.plusSeconds(60);
        }
        return candles;
    }

    public int fetchCallCount() {
        return fetchCalls.get();
    }

    private Candle syntheticCandle(Instrument instrument, Instant openTime) {
        return new Candle(
                instrument.symbol(), instrument.interval(), openTime, openTime.plusSeconds(59),
                new BigDecimal("100"), new BigDecimal("110"), new BigDecimal("90"), new BigDecimal("105"),
                new BigDecimal("10"), new BigDecimal("1000"), 50, "STUB"
        );
    }
}
