package com.btcassistant.event;

import com.btcassistant.feature.FeatureCalculatedEvent;
import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import com.btcassistant.feature.MarketFeatureEntity;
import com.btcassistant.feature.MarketFeatureRepository;
import com.btcassistant.market.Candle;
import com.btcassistant.market.CandleEntity;
import com.btcassistant.market.CandleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventDetectorTest {

    private static final Instant OPEN_TIME = Instant.parse("2026-01-01T00:00:00Z");
    private static final FeatureCalculatedEvent EVENT = new FeatureCalculatedEvent("BTCUSDT", "1m", OPEN_TIME, "1.0");

    @Mock
    private MarketFeatureRepository featureRepository;
    @Mock
    private CandleRepository candleRepository;
    @Mock
    private EventPersister eventPersister;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private static Candle candle(BigDecimal close) {
        return new Candle("BTCUSDT", "1m", OPEN_TIME, OPEN_TIME.plusSeconds(59),
                close, close, close, close, new BigDecimal("10"), new BigDecimal("1000"), 1, "BINANCE_WS");
    }

    private static FeatureSnapshot snapshot(Map<String, String> values) {
        java.util.HashMap<String, BigDecimal> decimals = new java.util.HashMap<>();
        values.forEach((k, v) -> decimals.put(k, new BigDecimal(v)));
        return new FeatureSnapshot("BTCUSDT", "1m", OPEN_TIME, "1.0", decimals);
    }

    @Test
    void persistsOneDetectedEventPerMatchingRule() {
        when(featureRepository.findBySymbolAndIntervalAndCandleOpenTimeAndFeatureVersion("BTCUSDT", "1m", OPEN_TIME, "1.0"))
                .thenReturn(Optional.of(MarketFeatureEntity.from(snapshot(Map.of(FeatureNames.RETURN_15M, "-0.05")))));
        when(candleRepository.findBySymbolAndIntervalAndOpenTime("BTCUSDT", "1m", OPEN_TIME))
                .thenReturn(Optional.of(CandleEntity.from(candle(new BigDecimal("100")))));
        when(featureRepository.findTop5BySymbolAndIntervalAndFeatureVersionAndCandleOpenTimeLessThanOrderByCandleOpenTimeDesc(
                "BTCUSDT", "1m", "1.0", OPEN_TIME)).thenReturn(List.of());
        when(eventPersister.persist(any(DetectedEvent.class))).thenReturn(UUID.randomUUID());

        EventProperties properties = new EventProperties();
        EventDetector detector = new EventDetector(featureRepository, candleRepository,
                List.of(new RapidDropRule(properties), new RapidRiseRule(properties)), eventPersister, eventPublisher);

        detector.onFeatureCalculated(EVENT);

        verify(eventPersister).persist(any(DetectedEvent.class));
    }

    @Test
    void skipsWhenFeatureSnapshotNotFound() {
        when(featureRepository.findBySymbolAndIntervalAndCandleOpenTimeAndFeatureVersion("BTCUSDT", "1m", OPEN_TIME, "1.0"))
                .thenReturn(Optional.empty());

        EventDetector detector = new EventDetector(featureRepository, candleRepository, List.of(), eventPersister, eventPublisher);

        detector.onFeatureCalculated(EVENT);

        verify(eventPersister, never()).persist(any());
    }

    @Test
    void skipsWhenCandleNotFound() {
        when(featureRepository.findBySymbolAndIntervalAndCandleOpenTimeAndFeatureVersion("BTCUSDT", "1m", OPEN_TIME, "1.0"))
                .thenReturn(Optional.of(MarketFeatureEntity.from(snapshot(Map.of()))));
        when(candleRepository.findBySymbolAndIntervalAndOpenTime("BTCUSDT", "1m", OPEN_TIME)).thenReturn(Optional.empty());

        EventDetector detector = new EventDetector(featureRepository, candleRepository, List.of(), eventPersister, eventPublisher);

        detector.onFeatureCalculated(EVENT);

        verify(eventPersister, never()).persist(any());
    }
}
