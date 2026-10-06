package com.btcassistant.market;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntervalSupportTest {

    @ParameterizedTest
    @CsvSource({
            "1m, PT1M",
            "5m, PT5M",
            "15m, PT15M",
            "30m, PT30M",
            "1h, PT1H",
            "4h, PT4H",
            "12h, PT12H",
            "24h, PT24H"
    })
    void convertsSupportedIntervals(String interval, String expectedIso) {
        assertThat(IntervalSupport.toDuration(interval)).isEqualTo(Duration.parse(expectedIso));
    }

    @Test
    void rejectsUnsupportedInterval() {
        assertThatThrownBy(() -> IntervalSupport.toDuration("3m"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3m");
    }
}
