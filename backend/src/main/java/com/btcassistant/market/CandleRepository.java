package com.btcassistant.market;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CandleRepository extends JpaRepository<CandleEntity, Long> {

    Optional<CandleEntity> findBySymbolAndIntervalAndOpenTime(String symbol, String interval, Instant openTime);

    List<CandleEntity> findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
            String symbol, String interval, Instant from, Instant to);

    Optional<CandleEntity> findTopBySymbolAndIntervalOrderByOpenTimeDesc(String symbol, String interval);
}
