package com.btcassistant.feature;

/** Analisa volume na janela de {@code lookbackCandles} candles mais recentes (SDD §18). */
public interface VolumeAnalyzer {

    /** @return stats com todos os campos {@code null} se a janela tiver menos de {@code lookbackCandles} candles (comparabilidade entre snapshots). */
    VolumeStats analyze(RollingWindow window, int lookbackCandles);
}
