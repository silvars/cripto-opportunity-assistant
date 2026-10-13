package com.btcassistant.feature;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeaturePersisterTest {

    @Mock
    private MarketFeatureRepository featureRepository;

    private FeaturePersister persister;

    private static FeatureSnapshot snapshot() {
        return new FeatureSnapshot("BTCUSDT", "1m", Instant.parse("2026-01-01T00:00:00Z"), "1.0",
                Map.of(FeatureNames.RETURN_1M, new BigDecimal("0.01")));
    }

    @Test
    void persistsWhenNoExistingRowForKey() {
        persister = new FeaturePersister(featureRepository);
        when(featureRepository.findBySymbolAndIntervalAndCandleOpenTimeAndFeatureVersion(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        boolean persisted = persister.persist(snapshot());

        assertThat(persisted).isTrue();
        verify(featureRepository).save(any(MarketFeatureEntity.class));
    }

    @Test
    void skipsWhenRowAlreadyExistsForKey() {
        persister = new FeaturePersister(featureRepository);
        when(featureRepository.findBySymbolAndIntervalAndCandleOpenTimeAndFeatureVersion(any(), any(), any(), any()))
                .thenReturn(Optional.of(MarketFeatureEntity.from(snapshot())));

        boolean persisted = persister.persist(snapshot());

        assertThat(persisted).isFalse();
        verify(featureRepository, never()).save(any());
    }
}
