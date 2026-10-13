package com.btcassistant.feature;

import com.btcassistant.market.Candle;
import com.btcassistant.market.CandleEntity;
import com.btcassistant.market.CandleRepository;
import com.btcassistant.market.Instrument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Testa a orquestração completa do FeatureEngine com calculadores reais (SDD Fase 1 §6/§7). */
@ExtendWith(MockitoExtension.class)
class FeatureEngineTest {

    private static final Instrument INSTRUMENT = new Instrument("BTCUSDT", "BTC", "USDT", "BINANCE", "1m", true);
    private static final Instant REFERENCE_TIME = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private CandleRepository candleRepository;

    private FeatureEngine engine() {
        FeatureProperties properties = new FeatureProperties();
        properties.setVersion("1.0");
        properties.getWindows().setVolumeSma(20);
        return new FeatureEngine(candleRepository, properties,
                new SimpleReturnCalculator(), new PriceDeltaMomentumCalculator(), new StdDevVolatilityCalculator(),
                new SimpleExtremesAnalyzer(), new SimpleVolumeAnalyzer(new SimpleZScoreCalculator()));
    }

    /** 24h + 1 de candles de 1m com preço/volume constantes — todo horizonte encontra candle exato. */
    private static List<CandleEntity> flatHistory() {
        List<CandleEntity> entities = new ArrayList<>();
        for (int minutesAgo = 1440; minutesAgo >= 0; minutesAgo--) {
            Instant openTime = REFERENCE_TIME.minus(Duration.ofMinutes(minutesAgo));
            Candle candle = new Candle("BTCUSDT", "1m", openTime, openTime.plusSeconds(59),
                    new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"),
                    new BigDecimal("10"), new BigDecimal("1000"), 1, "BINANCE_WS");
            entities.add(CandleEntity.from(candle));
        }
        return entities;
    }

    @Test
    void calculatesCompleteSnapshotWithFlatPriceHistory() {
        when(candleRepository.findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                any(), any(), any(), any())).thenReturn(flatHistory());

        FeatureSnapshot snapshot = engine().calculate(INSTRUMENT, REFERENCE_TIME);

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.symbol()).isEqualTo("BTCUSDT");
        assertThat(snapshot.featureVersion()).isEqualTo("1.0");
        assertThat(snapshot.candleOpenTime()).isEqualTo(REFERENCE_TIME);

        assertThat(snapshot.values().get(FeatureNames.RETURN_1M)).isEqualByComparingTo("0");
        assertThat(snapshot.values().get(FeatureNames.RETURN_24H)).isEqualByComparingTo("0");
        assertThat(snapshot.values().get(FeatureNames.MOMENTUM_4H)).isEqualByComparingTo("0");
        assertThat(snapshot.values().get(FeatureNames.VOLATILITY_1H)).isEqualByComparingTo("0");
        assertThat(snapshot.values().get(FeatureNames.DISTANCE_RECENT_HIGH_24H)).isEqualByComparingTo("0");
        assertThat(snapshot.values().get(FeatureNames.DISTANCE_RECENT_LOW_24H)).isEqualByComparingTo("0");
        assertThat(snapshot.values().get(FeatureNames.DRAWDOWN_FROM_RECENT_HIGH)).isEqualByComparingTo("0");
        assertThat(snapshot.values().get(FeatureNames.RECOVERY_FROM_RECENT_LOW)).isEqualByComparingTo("0");
        assertThat(snapshot.values().get(FeatureNames.VOLUME_SMA)).isEqualByComparingTo("10");
        assertThat(snapshot.values().get(FeatureNames.VOLUME_RATIO)).isEqualByComparingTo("1");
        assertThat(snapshot.values().get(FeatureNames.VOLUME_ZSCORE)).isNull();
    }

    @Test
    void returnsNullWhenNoCandleExistsAtReferenceTime() {
        when(candleRepository.findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                any(), any(), any(), any())).thenReturn(List.of());

        assertThat(engine().calculate(INSTRUMENT, REFERENCE_TIME)).isNull();
    }

    @Test
    void returnsNullWhenLastCandleReturnedIsNotTheReferenceCandle() {
        // simula uma implementação de repositório que devolveu candles além do limite requerido
        List<CandleEntity> history = flatHistory();
        when(candleRepository.findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                any(), any(), any(), any())).thenReturn(history.subList(0, history.size() - 1));

        assertThat(engine().calculate(INSTRUMENT, REFERENCE_TIME)).isNull();
    }
}
