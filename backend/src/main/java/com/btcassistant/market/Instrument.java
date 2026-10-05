package com.btcassistant.market;

/** Instrumento negociado — symbol/exchange nunca são hardcoded no domínio (ADR-008). */
public record Instrument(
    String symbol,
    String baseAsset,
    String quoteAsset,
    String exchange,
    String interval,
    boolean enabled
) {
}
