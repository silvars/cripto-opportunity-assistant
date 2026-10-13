package com.btcassistant.feature;

import java.math.BigDecimal;
import java.time.Duration;

/** Desvio padrão amostral dos retornos de 1 candle dentro da janela {@code horizon} (SDD §18). */
public interface VolatilityCalculator {

    /** @return desvio padrão amostral dos retornos consecutivos na janela, ou {@code null} se houver menos de 2 retornos disponíveis (histórico insuficiente). */
    BigDecimal calculate(RollingWindow window, Duration horizon);
}
