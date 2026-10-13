package com.btcassistant.feature;

import com.btcassistant.market.Candle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

@Component
public class SimpleVolumeAnalyzer implements VolumeAnalyzer {

    private final ZScoreCalculator zScoreCalculator;

    public SimpleVolumeAnalyzer(ZScoreCalculator zScoreCalculator) {
        this.zScoreCalculator = zScoreCalculator;
    }

    @Override
    public VolumeStats analyze(RollingWindow window, int lookbackCandles) {
        List<Candle> candles = window.lastN(lookbackCandles);
        if (candles.size() < lookbackCandles) {
            return new VolumeStats(null, null, null);
        }

        List<BigDecimal> volumes = candles.stream().map(Candle::volume).toList();
        BigDecimal sma = StatisticsSupport.mean(volumes);
        BigDecimal volumeNow = window.reference().volume();

        BigDecimal ratio = sma.signum() == 0 ? null : volumeNow.divide(sma, MathContext.DECIMAL64);
        BigDecimal zscore = zScoreCalculator.zscore(volumeNow, volumes);

        return new VolumeStats(sma, ratio, zscore);
    }
}
