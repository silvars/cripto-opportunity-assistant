package com.btcassistant.market;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CandleRepository extends JpaRepository<CandleEntity, Long> {

    Optional<CandleEntity> findBySymbolAndIntervalAndOpenTime(String symbol, String interval, Instant openTime);

    List<CandleEntity> findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
            String symbol, String interval, Instant from, Instant to);

    Optional<CandleEntity> findTopBySymbolAndIntervalOrderByOpenTimeDesc(String symbol, String interval);

    Optional<CandleEntity> findTopBySymbolAndIntervalOrderByOpenTimeAsc(String symbol, String interval);

    long countBySymbolAndInterval(String symbol, String interval);

    @Query("select c.openTime from CandleEntity c "
            + "where c.symbol = :symbol and c.interval = :interval and c.openTime between :from and :to "
            + "order by c.openTime asc")
    List<Instant> findOpenTimesInRange(@Param("symbol") String symbol, @Param("interval") String interval,
                                        @Param("from") Instant from, @Param("to") Instant to);
}
