package com.btcassistant.market.web;

import java.math.BigDecimal;
import java.time.Instant;

/** DTO de API — nunca expor CandleEntity diretamente (SDD §70). */
public record CandleResponse(
    String symbol,
    String interval,
    Instant openTime,
    Instant closeTime,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal volume
) {
}
