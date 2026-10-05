package com.btcassistant.market.web;

import com.btcassistant.market.CandleEntity;
import com.btcassistant.market.CandleRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/market")
public class MarketController {

    private final CandleRepository candleRepository;

    public MarketController(CandleRepository candleRepository) {
        this.candleRepository = candleRepository;
    }

    @GetMapping("/candles")
    public List<CandleResponse> candles(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "1m") String interval,
            @RequestParam Instant from,
            @RequestParam Instant to
    ) {
        return candleRepository
                .findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(symbol, interval, from, to)
                .stream()
                .map(MarketController::toResponse)
                .toList();
    }

    @GetMapping("/{symbol}/latest")
    public CandleResponse latest(@PathVariable String symbol, @RequestParam(defaultValue = "1m") String interval) {
        return candleRepository.findTopBySymbolAndIntervalOrderByOpenTimeDesc(symbol, interval)
                .map(MarketController::toResponse)
                .orElse(null);
    }

    private static CandleResponse toResponse(CandleEntity entity) {
        return new CandleResponse(
                entity.getSymbol(),
                entity.getInterval(),
                entity.getOpenTime(),
                entity.getCloseTime(),
                entity.getOpen(),
                entity.getHigh(),
                entity.getLow(),
                entity.getClose(),
                entity.getVolume()
        );
    }
}
