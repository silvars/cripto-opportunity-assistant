package com.btcassistant.feature;

import com.btcassistant.market.CandleEntity;
import com.btcassistant.market.CandleRepository;
import com.btcassistant.market.Instrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquestra o cálculo de todas as features de um candle (SDD Fase 1). Carrega apenas candles com
 * {@code openTime <= atCandleOpenTime} — garante no-lookahead estruturalmente (SDD §2.2), não por
 * disciplina de quem chama.
 */
@Component
public class FeatureEngine implements FeatureCalculator {

    private static final Logger log = LoggerFactory.getLogger(FeatureEngine.class);
    private static final Duration MAX_HORIZON = Duration.ofHours(24);

    private static final List<Duration> RETURN_HORIZONS = List.of(
            Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofMinutes(30),
            Duration.ofHours(1), Duration.ofHours(4), Duration.ofHours(12), Duration.ofHours(24));
    private static final List<Duration> MOMENTUM_HORIZONS = List.of(
            Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofHours(1), Duration.ofHours(4));
    private static final List<Duration> VOLATILITY_HORIZONS = List.of(
            Duration.ofMinutes(15), Duration.ofHours(1), Duration.ofHours(4), Duration.ofHours(24));
    private static final List<Duration> EXTREMES_HORIZONS = List.of(
            Duration.ofHours(1), Duration.ofHours(4), Duration.ofHours(24));

    private final CandleRepository candleRepository;
    private final FeatureProperties featureProperties;
    private final ReturnCalculator returnCalculator;
    private final MomentumCalculator momentumCalculator;
    private final VolatilityCalculator volatilityCalculator;
    private final ExtremesAnalyzer extremesAnalyzer;
    private final VolumeAnalyzer volumeAnalyzer;

    public FeatureEngine(CandleRepository candleRepository,
                         FeatureProperties featureProperties,
                         ReturnCalculator returnCalculator,
                         MomentumCalculator momentumCalculator,
                         VolatilityCalculator volatilityCalculator,
                         ExtremesAnalyzer extremesAnalyzer,
                         VolumeAnalyzer volumeAnalyzer) {
        this.candleRepository = candleRepository;
        this.featureProperties = featureProperties;
        this.returnCalculator = returnCalculator;
        this.momentumCalculator = momentumCalculator;
        this.volatilityCalculator = volatilityCalculator;
        this.extremesAnalyzer = extremesAnalyzer;
        this.volumeAnalyzer = volumeAnalyzer;
    }

    @Override
    public String featureVersion() {
        return featureProperties.getVersion();
    }

    @Override
    public FeatureSnapshot calculate(Instrument instrument, Instant atCandleOpenTime) {
        String symbol = instrument.symbol();
        String interval = instrument.interval();

        List<CandleEntity> entities = candleRepository.findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                symbol, interval, atCandleOpenTime.minus(MAX_HORIZON), atCandleOpenTime);
        if (entities.isEmpty() || !entities.get(entities.size() - 1).getOpenTime().equals(atCandleOpenTime)) {
            log.warn("feature_calculation_failed symbol={} openTime={} reason=reference-candle-not-found", symbol, atCandleOpenTime);
            return null;
        }

        RollingWindow window = RollingWindow.of(entities.stream().map(CandleEntity::toCandle).toList());
        Map<String, BigDecimal> values = new HashMap<>();

        values.put(FeatureNames.RETURN_1M, returnCalculator.calculate(window, RETURN_HORIZONS.get(0)));
        values.put(FeatureNames.RETURN_5M, returnCalculator.calculate(window, RETURN_HORIZONS.get(1)));
        values.put(FeatureNames.RETURN_15M, returnCalculator.calculate(window, RETURN_HORIZONS.get(2)));
        values.put(FeatureNames.RETURN_30M, returnCalculator.calculate(window, RETURN_HORIZONS.get(3)));
        values.put(FeatureNames.RETURN_1H, returnCalculator.calculate(window, RETURN_HORIZONS.get(4)));
        values.put(FeatureNames.RETURN_4H, returnCalculator.calculate(window, RETURN_HORIZONS.get(5)));
        values.put(FeatureNames.RETURN_12H, returnCalculator.calculate(window, RETURN_HORIZONS.get(6)));
        values.put(FeatureNames.RETURN_24H, returnCalculator.calculate(window, RETURN_HORIZONS.get(7)));

        values.put(FeatureNames.MOMENTUM_5M, momentumCalculator.calculate(window, MOMENTUM_HORIZONS.get(0)));
        values.put(FeatureNames.MOMENTUM_15M, momentumCalculator.calculate(window, MOMENTUM_HORIZONS.get(1)));
        values.put(FeatureNames.MOMENTUM_1H, momentumCalculator.calculate(window, MOMENTUM_HORIZONS.get(2)));
        values.put(FeatureNames.MOMENTUM_4H, momentumCalculator.calculate(window, MOMENTUM_HORIZONS.get(3)));

        values.put(FeatureNames.VOLATILITY_15M, volatilityCalculator.calculate(window, VOLATILITY_HORIZONS.get(0)));
        values.put(FeatureNames.VOLATILITY_1H, volatilityCalculator.calculate(window, VOLATILITY_HORIZONS.get(1)));
        values.put(FeatureNames.VOLATILITY_4H, volatilityCalculator.calculate(window, VOLATILITY_HORIZONS.get(2)));
        values.put(FeatureNames.VOLATILITY_24H, volatilityCalculator.calculate(window, VOLATILITY_HORIZONS.get(3)));

        values.put(FeatureNames.DISTANCE_RECENT_HIGH_1H, extremesAnalyzer.distanceFromRecentHigh(window, EXTREMES_HORIZONS.get(0)));
        values.put(FeatureNames.DISTANCE_RECENT_HIGH_4H, extremesAnalyzer.distanceFromRecentHigh(window, EXTREMES_HORIZONS.get(1)));
        values.put(FeatureNames.DISTANCE_RECENT_HIGH_24H, extremesAnalyzer.distanceFromRecentHigh(window, EXTREMES_HORIZONS.get(2)));
        values.put(FeatureNames.DISTANCE_RECENT_LOW_1H, extremesAnalyzer.distanceFromRecentLow(window, EXTREMES_HORIZONS.get(0)));
        values.put(FeatureNames.DISTANCE_RECENT_LOW_4H, extremesAnalyzer.distanceFromRecentLow(window, EXTREMES_HORIZONS.get(1)));
        values.put(FeatureNames.DISTANCE_RECENT_LOW_24H, extremesAnalyzer.distanceFromRecentLow(window, EXTREMES_HORIZONS.get(2)));

        // SDD não define janela própria para "drawdown/recovery" além de 1h/4h/24h já cobertas pelos
        // extremos — reaproveita a maior janela configurada (24h) sob um nome de significado mais amplo.
        values.put(FeatureNames.DRAWDOWN_FROM_RECENT_HIGH, values.get(FeatureNames.DISTANCE_RECENT_HIGH_24H));
        values.put(FeatureNames.RECOVERY_FROM_RECENT_LOW, values.get(FeatureNames.DISTANCE_RECENT_LOW_24H));

        VolumeStats volumeStats = volumeAnalyzer.analyze(window, featureProperties.getWindows().getVolumeSma());
        values.put(FeatureNames.VOLUME_SMA, volumeStats.sma());
        values.put(FeatureNames.VOLUME_RATIO, volumeStats.ratio());
        values.put(FeatureNames.VOLUME_ZSCORE, volumeStats.zscore());

        log.info("feature_calculated symbol={} interval={} openTime={} featureVersion={}",
                symbol, interval, atCandleOpenTime, featureVersion());

        return new FeatureSnapshot(symbol, interval, atCandleOpenTime, featureVersion(), values);
    }
}
