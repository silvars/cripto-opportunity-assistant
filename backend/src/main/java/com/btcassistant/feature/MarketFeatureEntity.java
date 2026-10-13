package com.btcassistant.feature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Entidade de persistência — nunca exposta diretamente na API (SDD §70). Linhas são imutáveis: mudança de fórmula gera novo {@code featureVersion}, nunca sobrescreve (SDD §19). */
@Entity
@Table(name = "market_features", uniqueConstraints = @UniqueConstraint(
        name = "uq_feature", columnNames = {"symbol", "interval", "candle_open_time", "feature_version"}))
public class MarketFeatureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, length = 10)
    private String interval;

    @Column(name = "candle_open_time", nullable = false)
    private Instant candleOpenTime;

    @Column(name = "feature_version", nullable = false, length = 10)
    private String featureVersion;

    @Column(name = "return_1m", precision = 10, scale = 6)
    private BigDecimal return1m;
    @Column(name = "return_5m", precision = 10, scale = 6)
    private BigDecimal return5m;
    @Column(name = "return_15m", precision = 10, scale = 6)
    private BigDecimal return15m;
    @Column(name = "return_30m", precision = 10, scale = 6)
    private BigDecimal return30m;
    @Column(name = "return_1h", precision = 10, scale = 6)
    private BigDecimal return1h;
    @Column(name = "return_4h", precision = 10, scale = 6)
    private BigDecimal return4h;
    @Column(name = "return_12h", precision = 10, scale = 6)
    private BigDecimal return12h;
    @Column(name = "return_24h", precision = 10, scale = 6)
    private BigDecimal return24h;

    @Column(name = "volume_sma", precision = 24, scale = 8)
    private BigDecimal volumeSma;
    @Column(name = "volume_ratio", precision = 10, scale = 4)
    private BigDecimal volumeRatio;
    @Column(name = "volume_zscore", precision = 10, scale = 4)
    private BigDecimal volumeZscore;

    @Column(name = "volatility_15m", precision = 10, scale = 6)
    private BigDecimal volatility15m;
    @Column(name = "volatility_1h", precision = 10, scale = 6)
    private BigDecimal volatility1h;
    @Column(name = "volatility_4h", precision = 10, scale = 6)
    private BigDecimal volatility4h;
    @Column(name = "volatility_24h", precision = 10, scale = 6)
    private BigDecimal volatility24h;

    @Column(name = "distance_recent_high_1h", precision = 10, scale = 6)
    private BigDecimal distanceRecentHigh1h;
    @Column(name = "distance_recent_high_4h", precision = 10, scale = 6)
    private BigDecimal distanceRecentHigh4h;
    @Column(name = "distance_recent_high_24h", precision = 10, scale = 6)
    private BigDecimal distanceRecentHigh24h;
    @Column(name = "distance_recent_low_1h", precision = 10, scale = 6)
    private BigDecimal distanceRecentLow1h;
    @Column(name = "distance_recent_low_4h", precision = 10, scale = 6)
    private BigDecimal distanceRecentLow4h;
    @Column(name = "distance_recent_low_24h", precision = 10, scale = 6)
    private BigDecimal distanceRecentLow24h;

    @Column(name = "momentum_5m", precision = 10, scale = 6)
    private BigDecimal momentum5m;
    @Column(name = "momentum_15m", precision = 10, scale = 6)
    private BigDecimal momentum15m;
    @Column(name = "momentum_1h", precision = 10, scale = 6)
    private BigDecimal momentum1h;
    @Column(name = "momentum_4h", precision = 10, scale = 6)
    private BigDecimal momentum4h;

    @Column(name = "drawdown_from_recent_high", precision = 10, scale = 6)
    private BigDecimal drawdownFromRecentHigh;
    @Column(name = "recovery_from_recent_low", precision = 10, scale = 6)
    private BigDecimal recoveryFromRecentLow;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MarketFeatureEntity() {
        // JPA
    }

    public static MarketFeatureEntity from(FeatureSnapshot snapshot) {
        Map<String, BigDecimal> v = snapshot.values();
        MarketFeatureEntity entity = new MarketFeatureEntity();
        entity.symbol = snapshot.symbol();
        entity.interval = snapshot.interval();
        entity.candleOpenTime = snapshot.candleOpenTime();
        entity.featureVersion = snapshot.featureVersion();

        entity.return1m = v.get(FeatureNames.RETURN_1M);
        entity.return5m = v.get(FeatureNames.RETURN_5M);
        entity.return15m = v.get(FeatureNames.RETURN_15M);
        entity.return30m = v.get(FeatureNames.RETURN_30M);
        entity.return1h = v.get(FeatureNames.RETURN_1H);
        entity.return4h = v.get(FeatureNames.RETURN_4H);
        entity.return12h = v.get(FeatureNames.RETURN_12H);
        entity.return24h = v.get(FeatureNames.RETURN_24H);

        entity.volumeSma = v.get(FeatureNames.VOLUME_SMA);
        entity.volumeRatio = v.get(FeatureNames.VOLUME_RATIO);
        entity.volumeZscore = v.get(FeatureNames.VOLUME_ZSCORE);

        entity.volatility15m = v.get(FeatureNames.VOLATILITY_15M);
        entity.volatility1h = v.get(FeatureNames.VOLATILITY_1H);
        entity.volatility4h = v.get(FeatureNames.VOLATILITY_4H);
        entity.volatility24h = v.get(FeatureNames.VOLATILITY_24H);

        entity.distanceRecentHigh1h = v.get(FeatureNames.DISTANCE_RECENT_HIGH_1H);
        entity.distanceRecentHigh4h = v.get(FeatureNames.DISTANCE_RECENT_HIGH_4H);
        entity.distanceRecentHigh24h = v.get(FeatureNames.DISTANCE_RECENT_HIGH_24H);
        entity.distanceRecentLow1h = v.get(FeatureNames.DISTANCE_RECENT_LOW_1H);
        entity.distanceRecentLow4h = v.get(FeatureNames.DISTANCE_RECENT_LOW_4H);
        entity.distanceRecentLow24h = v.get(FeatureNames.DISTANCE_RECENT_LOW_24H);

        entity.momentum5m = v.get(FeatureNames.MOMENTUM_5M);
        entity.momentum15m = v.get(FeatureNames.MOMENTUM_15M);
        entity.momentum1h = v.get(FeatureNames.MOMENTUM_1H);
        entity.momentum4h = v.get(FeatureNames.MOMENTUM_4H);

        entity.drawdownFromRecentHigh = v.get(FeatureNames.DRAWDOWN_FROM_RECENT_HIGH);
        entity.recoveryFromRecentLow = v.get(FeatureNames.RECOVERY_FROM_RECENT_LOW);

        entity.createdAt = Instant.now();
        return entity;
    }

    /** Reconstrói o {@link FeatureSnapshot} de domínio a partir das colunas tipadas (inverso de {@link #from}). */
    public FeatureSnapshot toSnapshot() {
        Map<String, BigDecimal> values = new HashMap<>();
        values.put(FeatureNames.RETURN_1M, return1m);
        values.put(FeatureNames.RETURN_5M, return5m);
        values.put(FeatureNames.RETURN_15M, return15m);
        values.put(FeatureNames.RETURN_30M, return30m);
        values.put(FeatureNames.RETURN_1H, return1h);
        values.put(FeatureNames.RETURN_4H, return4h);
        values.put(FeatureNames.RETURN_12H, return12h);
        values.put(FeatureNames.RETURN_24H, return24h);
        values.put(FeatureNames.VOLUME_SMA, volumeSma);
        values.put(FeatureNames.VOLUME_RATIO, volumeRatio);
        values.put(FeatureNames.VOLUME_ZSCORE, volumeZscore);
        values.put(FeatureNames.VOLATILITY_15M, volatility15m);
        values.put(FeatureNames.VOLATILITY_1H, volatility1h);
        values.put(FeatureNames.VOLATILITY_4H, volatility4h);
        values.put(FeatureNames.VOLATILITY_24H, volatility24h);
        values.put(FeatureNames.DISTANCE_RECENT_HIGH_1H, distanceRecentHigh1h);
        values.put(FeatureNames.DISTANCE_RECENT_HIGH_4H, distanceRecentHigh4h);
        values.put(FeatureNames.DISTANCE_RECENT_HIGH_24H, distanceRecentHigh24h);
        values.put(FeatureNames.DISTANCE_RECENT_LOW_1H, distanceRecentLow1h);
        values.put(FeatureNames.DISTANCE_RECENT_LOW_4H, distanceRecentLow4h);
        values.put(FeatureNames.DISTANCE_RECENT_LOW_24H, distanceRecentLow24h);
        values.put(FeatureNames.MOMENTUM_5M, momentum5m);
        values.put(FeatureNames.MOMENTUM_15M, momentum15m);
        values.put(FeatureNames.MOMENTUM_1H, momentum1h);
        values.put(FeatureNames.MOMENTUM_4H, momentum4h);
        values.put(FeatureNames.DRAWDOWN_FROM_RECENT_HIGH, drawdownFromRecentHigh);
        values.put(FeatureNames.RECOVERY_FROM_RECENT_LOW, recoveryFromRecentLow);
        return new FeatureSnapshot(symbol, interval, candleOpenTime, featureVersion, values);
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

    public Instant getCandleOpenTime() {
        return candleOpenTime;
    }

    public String getFeatureVersion() {
        return featureVersion;
    }

    public BigDecimal getReturn1m() {
        return return1m;
    }

    public BigDecimal getReturn5m() {
        return return5m;
    }

    public BigDecimal getReturn15m() {
        return return15m;
    }

    public BigDecimal getReturn30m() {
        return return30m;
    }

    public BigDecimal getReturn1h() {
        return return1h;
    }

    public BigDecimal getReturn4h() {
        return return4h;
    }

    public BigDecimal getReturn12h() {
        return return12h;
    }

    public BigDecimal getReturn24h() {
        return return24h;
    }

    public BigDecimal getVolumeSma() {
        return volumeSma;
    }

    public BigDecimal getVolumeRatio() {
        return volumeRatio;
    }

    public BigDecimal getVolumeZscore() {
        return volumeZscore;
    }

    public BigDecimal getVolatility15m() {
        return volatility15m;
    }

    public BigDecimal getVolatility1h() {
        return volatility1h;
    }

    public BigDecimal getVolatility4h() {
        return volatility4h;
    }

    public BigDecimal getVolatility24h() {
        return volatility24h;
    }

    public BigDecimal getDistanceRecentHigh1h() {
        return distanceRecentHigh1h;
    }

    public BigDecimal getDistanceRecentHigh4h() {
        return distanceRecentHigh4h;
    }

    public BigDecimal getDistanceRecentHigh24h() {
        return distanceRecentHigh24h;
    }

    public BigDecimal getDistanceRecentLow1h() {
        return distanceRecentLow1h;
    }

    public BigDecimal getDistanceRecentLow4h() {
        return distanceRecentLow4h;
    }

    public BigDecimal getDistanceRecentLow24h() {
        return distanceRecentLow24h;
    }

    public BigDecimal getMomentum5m() {
        return momentum5m;
    }

    public BigDecimal getMomentum15m() {
        return momentum15m;
    }

    public BigDecimal getMomentum1h() {
        return momentum1h;
    }

    public BigDecimal getMomentum4h() {
        return momentum4h;
    }

    public BigDecimal getDrawdownFromRecentHigh() {
        return drawdownFromRecentHigh;
    }

    public BigDecimal getRecoveryFromRecentLow() {
        return recoveryFromRecentLow;
    }
}
