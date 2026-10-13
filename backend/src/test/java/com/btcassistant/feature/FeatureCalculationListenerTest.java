package com.btcassistant.feature;

import com.btcassistant.market.CandlePersistedEvent;
import com.btcassistant.market.Instrument;
import com.btcassistant.market.MarketProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureCalculationListenerTest {

    private static final Instrument INSTRUMENT = new Instrument("BTCUSDT", "BTC", "USDT", "BINANCE", "1m", true);
    private static final Instant OPEN_TIME = Instant.parse("2026-01-01T00:00:00Z");
    private static final CandlePersistedEvent EVENT = new CandlePersistedEvent("BTCUSDT", "1m", OPEN_TIME);

    @Mock
    private MarketProperties marketProperties;
    @Mock
    private FeatureCalculator featureCalculator;
    @Mock
    private FeaturePersister featurePersister;

    private FeatureCalculationListener listener() {
        return new FeatureCalculationListener(marketProperties, featureCalculator, featurePersister);
    }

    @Test
    void skipsWhenInstrumentNotConfigured() {
        when(marketProperties.findEnabled("BTCUSDT", "1m")).thenReturn(null);

        listener().onCandlePersisted(EVENT);

        verify(featureCalculator, never()).calculate(any(), any());
        verify(featurePersister, never()).persist(any());
    }

    @Test
    void skipsPersistenceWhenSnapshotCouldNotBeCalculated() {
        when(marketProperties.findEnabled("BTCUSDT", "1m")).thenReturn(INSTRUMENT);
        when(featureCalculator.calculate(INSTRUMENT, OPEN_TIME)).thenReturn(null);

        listener().onCandlePersisted(EVENT);

        verify(featurePersister, never()).persist(any());
    }

    @Test
    void persistsSnapshotWhenCalculated() {
        FeatureSnapshot snapshot = new FeatureSnapshot("BTCUSDT", "1m", OPEN_TIME, "1.0",
                Map.of(FeatureNames.RETURN_1M, new BigDecimal("0.01")));
        when(marketProperties.findEnabled("BTCUSDT", "1m")).thenReturn(INSTRUMENT);
        when(featureCalculator.calculate(INSTRUMENT, OPEN_TIME)).thenReturn(snapshot);

        listener().onCandlePersisted(EVENT);

        verify(featurePersister).persist(snapshot);
    }
}
