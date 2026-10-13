package com.btcassistant.feature;

import java.math.BigDecimal;
import java.time.Duration;

/** Diferença absoluta de preço entre o candle de referência e {@code horizon} atrás (SDD §18) — distinto de {@link ReturnCalculator}, que é percentual. */
public interface MomentumCalculator {

    /** @return {@code close_now - close_horizon_ago}, ou {@code null} se não houver candle exatamente {@code horizon} atrás. */
    BigDecimal calculate(RollingWindow window, Duration horizon);
}
