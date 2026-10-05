package com.btcassistant.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

/** Entidade de persistência — nunca exposta diretamente na API (SDD §70). */
@Entity
@Table(name = "candles", uniqueConstraints = @UniqueConstraint(name = "uq_candle", columnNames = {"symbol", "interval", "open_time"}))
public class CandleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, length = 10)
    private String interval;

    @Column(name = "open_time", nullable = false)
    private Instant openTime;

    @Column(name = "close_time", nullable = false)
    private Instant closeTime;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal open;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal high;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal low;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal close;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal volume;

    @Column(name = "quote_volume", nullable = false, precision = 24, scale = 8)
    private BigDecimal quoteVolume;

    @Column(name = "trade_count", nullable = false)
    private int tradeCount;

    @Column(nullable = false, length = 20)
    private String source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CandleEntity() {
        // JPA
    }

    public static CandleEntity from(Candle candle) {
        CandleEntity entity = new CandleEntity();
        entity.symbol = candle.symbol();
        entity.interval = candle.interval();
        entity.openTime = candle.openTime();
        entity.closeTime = candle.closeTime();
        entity.open = candle.open();
        entity.high = candle.high();
        entity.low = candle.low();
        entity.close = candle.close();
        entity.volume = candle.volume();
        entity.quoteVolume = candle.quoteVolume();
        entity.tradeCount = candle.tradeCount();
        entity.source = candle.source();
        entity.createdAt = Instant.now();
        return entity;
    }

    public Candle toCandle() {
        return new Candle(symbol, interval, openTime, closeTime, open, high, low, close, volume, quoteVolume, tradeCount, source);
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getInterval() {
        return interval;
    }

    public Instant getOpenTime() {
        return openTime;
    }

    public Instant getCloseTime() {
        return closeTime;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public BigDecimal getClose() {
        return close;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public BigDecimal getQuoteVolume() {
        return quoteVolume;
    }

    public int getTradeCount() {
        return tradeCount;
    }

    public String getSource() {
        return source;
    }
}
