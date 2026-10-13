package com.btcassistant.feature;

import java.math.BigDecimal;
import java.time.Duration;

/** Distância percentual do preço atual em relação à máxima/mínima recente na janela (SDD §18). */
public interface ExtremesAnalyzer {

    /** @return {@code (close_now - highest_high) / highest_high} — negativo/zero (preço no ou abaixo da máxima recente). */
    BigDecimal distanceFromRecentHigh(RollingWindow window, Duration horizon);

    /** @return {@code (close_now - lowest_low) / lowest_low} — positivo/zero (preço no ou acima da mínima recente). */
    BigDecimal distanceFromRecentLow(RollingWindow window, Duration horizon);
}
