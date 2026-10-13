package com.btcassistant.feature;

import java.math.BigDecimal;

/** Média móvel simples, razão e z-score do volume na janela configurada (SDD §18). */
public record VolumeStats(BigDecimal sma, BigDecimal ratio, BigDecimal zscore) {
}
