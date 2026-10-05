package com.btcassistant.market.binance;

import com.btcassistant.market.Candle;
import com.btcassistant.market.Instrument;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;

/** Converte payloads da Binance (WS/REST) para o record de domínio Candle. */
final class BinanceKlineMapper {

    private BinanceKlineMapper() {
    }

    static Candle fromWebSocket(Instrument instrument, JsonNode kline) {
        return new Candle(
                instrument.symbol(),
                instrument.interval(),
                Instant.ofEpochMilli(kline.path("t").asLong()),
                Instant.ofEpochMilli(kline.path("T").asLong()),
                new BigDecimal(kline.path("o").asText()),
                new BigDecimal(kline.path("h").asText()),
                new BigDecimal(kline.path("l").asText()),
                new BigDecimal(kline.path("c").asText()),
                new BigDecimal(kline.path("v").asText()),
                new BigDecimal(kline.path("q").asText()),
                kline.path("n").asInt(),
                "BINANCE_WS"
        );
    }

    static Candle fromRest(Instrument instrument, JsonNode row) {
        return new Candle(
                instrument.symbol(),
                instrument.interval(),
                Instant.ofEpochMilli(row.get(0).asLong()),
                Instant.ofEpochMilli(row.get(6).asLong()),
                new BigDecimal(row.get(1).asText()),
                new BigDecimal(row.get(2).asText()),
                new BigDecimal(row.get(3).asText()),
                new BigDecimal(row.get(4).asText()),
                new BigDecimal(row.get(5).asText()),
                new BigDecimal(row.get(7).asText()),
                row.get(8).asInt(),
                "BINANCE_REST"
        );
    }
}
