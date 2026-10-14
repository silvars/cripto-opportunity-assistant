package com.btcassistant.research;

import com.btcassistant.opportunity.Classification;
import com.btcassistant.opportunity.Direction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Alerta ignorado (ou sem decisão) cujo mercado se moveu a favor da direção do alerta (SDD §56). */
public record MissedOpportunity(
    UUID alertId,
    String symbol,
    Direction direction,
    Classification classification,
    BigDecimal score,
    BigDecimal canonicalHorizonReturn,
    BigDecimal favorableMove,
    Instant createdAt
) {
}
