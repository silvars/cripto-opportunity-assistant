package com.btcassistant.market;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackfillPeriodTest {

    private static final Instant NOW = Instant.parse("2026-10-06T00:00:00Z");

    @Test
    void resolvesMonths() {
        assertThat(BackfillPeriod.resolveStart("12-months", NOW)).isEqualTo(Instant.parse("2025-10-06T00:00:00Z"));
    }

    @Test
    void resolvesDays() {
        assertThat(BackfillPeriod.resolveStart("2-days", NOW)).isEqualTo(Instant.parse("2026-10-04T00:00:00Z"));
    }

    @Test
    void resolvesYears() {
        assertThat(BackfillPeriod.resolveStart("1-years", NOW)).isEqualTo(Instant.parse("2025-10-06T00:00:00Z"));
    }

    @Test
    void rejectsMalformedPeriod() {
        assertThatThrownBy(() -> BackfillPeriod.resolveStart("12months", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnknownUnit() {
        assertThatThrownBy(() -> BackfillPeriod.resolveStart("12-weeks", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weeks");
    }
}
