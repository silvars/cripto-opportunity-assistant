package com.btcassistant.feature.web;

import java.math.BigDecimal;
import java.time.Instant;

/** DTO de API — nunca expõe a entidade JPA diretamente (SDD §70). */
public record FeatureResponse(
    String symbol,
    String interval,
    Instant candleOpenTime,
    String featureVersion,
    BigDecimal return1m,
    BigDecimal return5m,
    BigDecimal return15m,
    BigDecimal return30m,
    BigDecimal return1h,
    BigDecimal return4h,
    BigDecimal return12h,
    BigDecimal return24h,
    BigDecimal volumeSma,
    BigDecimal volumeRatio,
    BigDecimal volumeZscore,
    BigDecimal volatility15m,
    BigDecimal volatility1h,
    BigDecimal volatility4h,
    BigDecimal volatility24h,
    BigDecimal distanceRecentHigh1h,
    BigDecimal distanceRecentHigh4h,
    BigDecimal distanceRecentHigh24h,
    BigDecimal distanceRecentLow1h,
    BigDecimal distanceRecentLow4h,
    BigDecimal distanceRecentLow24h,
    BigDecimal momentum5m,
    BigDecimal momentum15m,
    BigDecimal momentum1h,
    BigDecimal momentum4h,
    BigDecimal drawdownFromRecentHigh,
    BigDecimal recoveryFromRecentLow
) {
}
