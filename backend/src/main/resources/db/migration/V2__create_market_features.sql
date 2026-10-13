CREATE TABLE market_features (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    interval VARCHAR(10) NOT NULL,
    candle_open_time TIMESTAMPTZ NOT NULL,
    feature_version VARCHAR(10) NOT NULL,
    return_1m NUMERIC(10,6), return_5m NUMERIC(10,6), return_15m NUMERIC(10,6),
    return_30m NUMERIC(10,6), return_1h NUMERIC(10,6), return_4h NUMERIC(10,6),
    return_12h NUMERIC(10,6), return_24h NUMERIC(10,6),
    volume_sma NUMERIC(24,8), volume_ratio NUMERIC(10,4), volume_zscore NUMERIC(10,4),
    volatility_15m NUMERIC(10,6), volatility_1h NUMERIC(10,6),
    volatility_4h NUMERIC(10,6), volatility_24h NUMERIC(10,6),
    distance_recent_high_1h NUMERIC(10,6), distance_recent_high_4h NUMERIC(10,6), distance_recent_high_24h NUMERIC(10,6),
    distance_recent_low_1h NUMERIC(10,6), distance_recent_low_4h NUMERIC(10,6), distance_recent_low_24h NUMERIC(10,6),
    momentum_5m NUMERIC(10,6), momentum_15m NUMERIC(10,6), momentum_1h NUMERIC(10,6), momentum_4h NUMERIC(10,6),
    drawdown_from_recent_high NUMERIC(10,6), recovery_from_recent_low NUMERIC(10,6),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_feature UNIQUE (symbol, interval, candle_open_time, feature_version)
);

CREATE INDEX idx_features_symbol_interval_time ON market_features (symbol, interval, candle_open_time DESC);
