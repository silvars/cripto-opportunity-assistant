package com.btcassistant.feature;

import com.btcassistant.market.Candle;

import java.math.BigDecimal;
import java.time.Instant;

/** Constrói candles 1m consecutivos para testes de features com dados sintéticos e valor esperado conhecido. */
final class FeatureTestSupport {

    static final Instant BASE_TIME = Instant.parse("2026-01-01T00:00:00Z");

    private FeatureTestSupport() {
    }

    static Candle candleAt(int minuteOffset, String close) {
        return candleAt(minuteOffset, close, close, close, "1");
    }

    static Candle candleAt(int minuteOffset, String close, String high, String low, String volume) {
        Instant openTime = BASE_TIME.plusSeconds(60L * minuteOffset);
        return new Candle("BTCUSDT", "1m", openTime, openTime.plusSeconds(59),
                new BigDecimal(close), new BigDecimal(high), new BigDecimal(low), new BigDecimal(close),
                new BigDecimal(volume), new BigDecimal(volume).multiply(new BigDecimal(close)), 1, "BINANCE_WS");
    }
}
