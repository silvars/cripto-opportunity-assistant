package com.btcassistant.feature;

import com.btcassistant.market.Candle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Component
public class PriceDeltaMomentumCalculator implements MomentumCalculator {

    @Override
    public BigDecimal calculate(RollingWindow window, Duration horizon) {
        Candle now = window.reference();
        Optional<Candle> past = window.at(now.openTime().minus(horizon));
        return past.map(candle -> now.close().subtract(candle.close())).orElse(null);
    }
}
