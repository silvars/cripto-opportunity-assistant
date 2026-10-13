package com.btcassistant.feature;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleVolumeAnalyzerTest {

    private final SimpleVolumeAnalyzer analyzer = new SimpleVolumeAnalyzer(new SimpleZScoreCalculator());

    @Test
    void calculatesSmaRatioAndZscoreOverLookbackWindow() {
        RollingWindow window = RollingWindow.of(List.of(
                FeatureTestSupport.candleAt(0, "100", "100", "100", "10"),
                FeatureTestSupport.candleAt(1, "100", "100", "100", "20"),
                FeatureTestSupport.candleAt(2, "100", "100", "100", "30")));

        VolumeStats stats = analyzer.analyze(window, 3);

        assertThat(stats.sma()).isEqualByComparingTo("20");
        assertThat(stats.ratio()).isEqualByComparingTo("1.5");
        assertThat(stats.zscore()).isNotNull();
    }

    @Test
    void returnsAllNullWhenWindowSmallerThanLookback() {
        RollingWindow window = RollingWindow.of(List.of(FeatureTestSupport.candleAt(0, "100", "100", "100", "10")));

        VolumeStats stats = analyzer.analyze(window, 3);

        assertThat(stats.sma()).isNull();
        assertThat(stats.ratio()).isNull();
        assertThat(stats.zscore()).isNull();
    }
}
