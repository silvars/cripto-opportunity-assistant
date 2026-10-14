package com.btcassistant.historical;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Um evento histórico candidato e sua similaridade com o evento atual (SDD §29). */
public record SimilarEvent(UUID eventId, Instant eventTime, BigDecimal similarityScore) {
}
