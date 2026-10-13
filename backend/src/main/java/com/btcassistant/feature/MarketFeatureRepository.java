package com.btcassistant.feature;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface MarketFeatureRepository extends JpaRepository<MarketFeatureEntity, Long> {

    Optional<MarketFeatureEntity> findBySymbolAndIntervalAndCandleOpenTimeAndFeatureVersion(
            String symbol, String interval, Instant candleOpenTime, String featureVersion);

    Optional<MarketFeatureEntity> findTopBySymbolAndIntervalAndFeatureVersionOrderByCandleOpenTimeDesc(
            String symbol, String interval, String featureVersion);
}
