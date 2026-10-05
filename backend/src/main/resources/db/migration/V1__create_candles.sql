-- V1__create_candles.sql
CREATE TABLE candles (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    interval VARCHAR(10) NOT NULL,
    open_time TIMESTAMPTZ NOT NULL,
    close_time TIMESTAMPTZ NOT NULL,
    open NUMERIC(20,8) NOT NULL,
    high NUMERIC(20,8) NOT NULL,
    low NUMERIC(20,8) NOT NULL,
    close NUMERIC(20,8) NOT NULL,
    volume NUMERIC(24,8) NOT NULL,
    quote_volume NUMERIC(24,8) NOT NULL,
    trade_count INTEGER NOT NULL,
    source VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_candle UNIQUE (symbol, interval, open_time)
);

CREATE INDEX idx_candles_symbol_interval_time ON candles (symbol, interval, open_time DESC);
