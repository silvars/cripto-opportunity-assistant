package com.btcassistant.feature;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MarketFeatureRepository extends JpaRepository<MarketFeatureEntity, Long> {

    Optional<MarketFeatureEntity> findBySymbolAndIntervalAndCandleOpenTimeAndFeatureVersion(
            String symbol, String interval, Instant candleOpenTime, String featureVersion);

    Optional<MarketFeatureEntity> findTopBySymbolAndIntervalAndFeatureVersionOrderByCandleOpenTimeDesc(
            String symbol, String interval, String featureVersion);

    /** Snapshots estritamente anteriores a {@code candleOpenTime} — usado para montar {@code FeatureHistory} (SDD Fase 2 §4). */
    List<MarketFeatureEntity> findTop5BySymbolAndIntervalAndFeatureVersionAndCandleOpenTimeLessThanOrderByCandleOpenTimeDesc(
            String symbol, String interval, String featureVersion, Instant candleOpenTime);
}
