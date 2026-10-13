package com.btcassistant.feature;

import com.btcassistant.market.Candle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.util.List;

@Component
public class SimpleExtremesAnalyzer implements ExtremesAnalyzer {

    @Override
    public BigDecimal distanceFromRecentHigh(RollingWindow window, Duration horizon) {
        List<Candle> candles = window.since(window.reference().openTime().minus(horizon));
        if (candles.isEmpty()) {
            return null;
        }
        BigDecimal highestHigh = candles.stream().map(Candle::high).max(BigDecimal::compareTo).orElseThrow();
        if (highestHigh.signum() == 0) {
            return null;
        }
        BigDecimal closeNow = window.reference().close();
        return closeNow.subtract(highestHigh).divide(highestHigh, MathContext.DECIMAL64);
    }

    @Override
    public BigDecimal distanceFromRecentLow(RollingWindow window, Duration horizon) {
        List<Candle> candles = window.since(window.reference().openTime().minus(horizon));
        if (candles.isEmpty()) {
            return null;
        }
        BigDecimal lowestLow = candles.stream().map(Candle::low).min(BigDecimal::compareTo).orElseThrow();
        if (lowestLow.signum() == 0) {
            return null;
        }
        BigDecimal closeNow = window.reference().close();
        return closeNow.subtract(lowestLow).divide(lowestLow, MathContext.DECIMAL64);
    }
}
