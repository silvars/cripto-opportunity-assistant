package com.btcassistant.research;

import java.math.BigDecimal;

/** Agregado determinístico sobre todos os eventos de um mesmo tipo, num horizonte (SDD §66). */
public record HorizonOutcomeStats(
    int sampleSize,
    BigDecimal meanFutureReturn,
    BigDecimal meanMfe,
    BigDecimal meanMae,
    BigDecimal targetBeforeStopRate
) {
}
