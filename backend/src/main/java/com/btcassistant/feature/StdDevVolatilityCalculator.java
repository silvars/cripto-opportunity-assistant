package com.btcassistant.feature;

import com.btcassistant.market.Candle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Desvio padrão amostral dos retornos de 1 candle dentro da janela — medida de dispersão
 * explicável e determinística (sem anualização; unidade compatível com {@code return_1m}).
 */
@Component
public class StdDevVolatilityCalculator implements VolatilityCalculator {

    @Override
    public BigDecimal calculate(RollingWindow window, Duration horizon) {
        List<Candle> candles = window.since(window.reference().openTime().minus(horizon));
        if (candles.size() < 3) {
            return null;
        }

        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            BigDecimal previousClose = candles.get(i - 1).close();
            if (previousClose.signum() == 0) {
                continue;
            }
            BigDecimal currentClose = candles.get(i).close();
            returns.add(currentClose.subtract(previousClose).divide(previousClose, MathContext.DECIMAL64));
        }

        if (returns.size() < 2) {
            return null;
        }

        return StatisticsSupport.sampleStdDev(returns);
    }
}
