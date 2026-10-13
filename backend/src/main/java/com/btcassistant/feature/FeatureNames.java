package com.btcassistant.feature;

/** Nomes das features — chaves do mapa {@link FeatureSnapshot#values()} e colunas de {@link MarketFeatureEntity}. */
public final class FeatureNames {

    public static final String RETURN_1M = "return_1m";
    public static final String RETURN_5M = "return_5m";
    public static final String RETURN_15M = "return_15m";
    public static final String RETURN_30M = "return_30m";
    public static final String RETURN_1H = "return_1h";
    public static final String RETURN_4H = "return_4h";
    public static final String RETURN_12H = "return_12h";
    public static final String RETURN_24H = "return_24h";

    public static final String VOLUME_SMA = "volume_sma";
    public static final String VOLUME_RATIO = "volume_ratio";
    public static final String VOLUME_ZSCORE = "volume_zscore";

    public static final String VOLATILITY_15M = "volatility_15m";
    public static final String VOLATILITY_1H = "volatility_1h";
    public static final String VOLATILITY_4H = "volatility_4h";
    public static final String VOLATILITY_24H = "volatility_24h";

    public static final String DISTANCE_RECENT_HIGH_1H = "distance_recent_high_1h";
    public static final String DISTANCE_RECENT_HIGH_4H = "distance_recent_high_4h";
    public static final String DISTANCE_RECENT_HIGH_24H = "distance_recent_high_24h";
    public static final String DISTANCE_RECENT_LOW_1H = "distance_recent_low_1h";
    public static final String DISTANCE_RECENT_LOW_4H = "distance_recent_low_4h";
    public static final String DISTANCE_RECENT_LOW_24H = "distance_recent_low_24h";

    public static final String MOMENTUM_5M = "momentum_5m";
    public static final String MOMENTUM_15M = "momentum_15m";
    public static final String MOMENTUM_1H = "momentum_1h";
    public static final String MOMENTUM_4H = "momentum_4h";

    public static final String DRAWDOWN_FROM_RECENT_HIGH = "drawdown_from_recent_high";
    public static final String RECOVERY_FROM_RECENT_LOW = "recovery_from_recent_low";

    private FeatureNames() {
    }
}
