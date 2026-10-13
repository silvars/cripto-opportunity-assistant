package com.btcassistant.market;

import java.time.Instant;

/** Publicado após um candle novo ser persistido via ingestão ao vivo (não durante backfill/gap recovery em lote). */
public record CandlePersistedEvent(String symbol, String interval, Instant openTime) {
}
