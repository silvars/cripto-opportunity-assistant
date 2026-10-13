package com.btcassistant.feature;

import java.math.BigDecimal;
import java.time.Duration;

/** Retorno percentual entre o candle de referência e o candle {@code horizon} atrás (SDD §18). */
public interface ReturnCalculator {

    /** @return {@code (close_now - close_horizon_ago) / close_horizon_ago}, ou {@code null} se não houver candle exatamente {@code horizon} atrás. */
    BigDecimal calculate(RollingWindow window, Duration horizon);
}
