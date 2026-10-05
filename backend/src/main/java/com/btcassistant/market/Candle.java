package com.btcassistant.market;

import java.math.BigDecimal;
import java.time.Instant;

/** Candle de domínio — nunca exposta diretamente como entidade JPA (SDD §70). */
public record Candle(
    String symbol,
    String interval,
    Instant openTime,
    Instant closeTime,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal volume,
    BigDecimal quoteVolume,
    int tradeCount,
    String source
) {
}
