package com.btcassistant.market;

import java.time.Instant;

/** Intervalo de tempo ausente no histórico persistido. */
public record HistoricalRange(Instant from, Instant to) {
}
