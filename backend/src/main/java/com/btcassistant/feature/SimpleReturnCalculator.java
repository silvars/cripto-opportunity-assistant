package com.btcassistant.feature;

import com.btcassistant.market.Candle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.util.Optional;

@Component
public class SimpleReturnCalculator implements ReturnCalculator {

    @Override
    public BigDecimal calculate(RollingWindow window, Duration horizon) {
        Candle now = window.reference();
        Optional<Candle> past = window.at(now.openTime().minus(horizon));
        if (past.isEmpty()) {
            return null;
        }

        BigDecimal closeNow = now.close();
        BigDecimal closePast = past.get().close();
        if (closePast.signum() == 0) {
            return null;
        }

        return closeNow.subtract(closePast).divide(closePast, MathContext.DECIMAL64);
    }
}
