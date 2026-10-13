package com.btcassistant.feature.web;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureProperties;
import com.btcassistant.feature.FeatureSnapshot;
import com.btcassistant.feature.MarketFeatureEntity;
import com.btcassistant.feature.MarketFeatureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureControllerTest {

    @Mock
    private MarketFeatureRepository featureRepository;

    private FeatureController controller() {
        FeatureProperties properties = new FeatureProperties();
        properties.setVersion("1.0");
        return new FeatureController(featureRepository, properties);
    }

    @Test
    void returnsMappedLatestFeatureSnapshot() {
        Instant openTime = Instant.parse("2026-01-01T00:00:00Z");
        FeatureSnapshot snapshot = new FeatureSnapshot("BTCUSDT", "1m", openTime, "1.0",
                Map.of(FeatureNames.RETURN_1M, new BigDecimal("0.01")));
        when(featureRepository.findTopBySymbolAndIntervalAndFeatureVersionOrderByCandleOpenTimeDesc("BTCUSDT", "1m", "1.0"))
                .thenReturn(Optional.of(MarketFeatureEntity.from(snapshot)));

        FeatureResponse response = controller().latest("BTCUSDT", "1m");

        assertThat(response.symbol()).isEqualTo("BTCUSDT");
        assertThat(response.featureVersion()).isEqualTo("1.0");
        assertThat(response.return1m()).isEqualByComparingTo("0.01");
    }

    @Test
    void returnsNullWhenNoFeatureSnapshotExists() {
        when(featureRepository.findTopBySymbolAndIntervalAndFeatureVersionOrderByCandleOpenTimeDesc("BTCUSDT", "1m", "1.0"))
                .thenReturn(Optional.empty());

        assertThat(controller().latest("BTCUSDT", "1m")).isNull();
    }
}
