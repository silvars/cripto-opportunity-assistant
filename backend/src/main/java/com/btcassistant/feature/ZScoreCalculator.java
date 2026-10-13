package com.btcassistant.feature;

import java.math.BigDecimal;
import java.util.List;

/** Quantos desvios padrão um valor está acima/abaixo da média de uma série (SDD §18). */
public interface ZScoreCalculator {

    /** @return {@code (value - mean) / stdDev}, ou {@code null} se a série tiver menos de 2 valores ou desvio padrão zero. */
    BigDecimal zscore(BigDecimal value, List<BigDecimal> series);
}
