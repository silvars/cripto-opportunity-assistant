package com.btcassistant.historical;

/** Chaves do mapa {@code normalized_features} — seis dimensões comparáveis entre eventos (SDD §27). */
public final class HistoricalFeatureNames {

    public static final String MAGNITUDE = "magnitude";
    public static final String DURATION = "duration";
    public static final String VOLUME_RATIO = "volumeRatio";
    public static final String VOLATILITY = "volatility";
    public static final String MOMENTUM = "momentum";
    public static final String DISTANCE_TO_EXTREME = "distanceToExtreme";

    private HistoricalFeatureNames() {
    }
}
