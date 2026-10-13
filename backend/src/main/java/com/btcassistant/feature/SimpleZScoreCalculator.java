package com.btcassistant.feature;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

@Component
public class SimpleZScoreCalculator implements ZScoreCalculator {

    @Override
    public BigDecimal zscore(BigDecimal value, List<BigDecimal> series) {
        if (series.size() < 2) {
            return null;
        }
        BigDecimal stdDev = StatisticsSupport.sampleStdDev(series);
        if (stdDev.signum() == 0) {
            return null;
        }
        BigDecimal mean = StatisticsSupport.mean(series);
        return value.subtract(mean).divide(stdDev, MathContext.DECIMAL64);
    }
}
