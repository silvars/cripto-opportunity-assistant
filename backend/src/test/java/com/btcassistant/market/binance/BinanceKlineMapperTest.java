package com.btcassistant.market.binance;

import com.btcassistant.market.Candle;
import com.btcassistant.market.Instrument;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BinanceKlineMapperTest {

    private static final Instrument INSTRUMENT = new Instrument("BTCUSDT", "BTC", "USDT", "BINANCE", "1m", true);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsWebSocketKlinePayload() {
        String json = """
                {
                  "t": 1735689600000,
                  "T": 1735689659999,
                  "o": "100.00000000",
                  "h": "110.00000000",
                  "l": "90.00000000",
                  "c": "105.00000000",
                  "v": "12.50000000",
                  "q": "1250.00000000",
                  "n": 42,
                  "x": true
                }
                """;
        JsonNode kline = objectMapper.readTree(json);

        Candle candle = BinanceKlineMapper.fromWebSocket(INSTRUMENT, kline);

        assertThat(candle.symbol()).isEqualTo("BTCUSDT");
        assertThat(candle.interval()).isEqualTo("1m");
        assertThat(candle.openTime()).isEqualTo(Instant.ofEpochMilli(1735689600000L));
        assertThat(candle.closeTime()).isEqualTo(Instant.ofEpochMilli(1735689659999L));
        assertThat(candle.open()).isEqualByComparingTo(new BigDecimal("100.00000000"));
        assertThat(candle.high()).isEqualByComparingTo(new BigDecimal("110.00000000"));
        assertThat(candle.low()).isEqualByComparingTo(new BigDecimal("90.00000000"));
        assertThat(candle.close()).isEqualByComparingTo(new BigDecimal("105.00000000"));
        assertThat(candle.volume()).isEqualByComparingTo(new BigDecimal("12.50000000"));
        assertThat(candle.quoteVolume()).isEqualByComparingTo(new BigDecimal("1250.00000000"));
        assertThat(candle.tradeCount()).isEqualTo(42);
        assertThat(candle.source()).isEqualTo("BINANCE_WS");
    }

    @Test
    void mapsRestKlineRow() {
        String json = "[1735689600000,\"100.0\",\"110.0\",\"90.0\",\"105.0\",\"12.5\","
                + "1735689659999,\"1250.0\",42,\"0\",\"0\",\"0\"]";
        JsonNode row = objectMapper.readTree(json);

        Candle candle = BinanceKlineMapper.fromRest(INSTRUMENT, row);

        assertThat(candle.openTime()).isEqualTo(Instant.ofEpochMilli(1735689600000L));
        assertThat(candle.closeTime()).isEqualTo(Instant.ofEpochMilli(1735689659999L));
        assertThat(candle.open()).isEqualByComparingTo(new BigDecimal("100.0"));
        assertThat(candle.tradeCount()).isEqualTo(42);
        assertThat(candle.source()).isEqualTo("BINANCE_REST");
    }
}
