package com.btcassistant.market;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricalCoverageCheckerTest {

    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant TO = FROM.plus(Duration.ofMinutes(10));

    @Mock
    private CandleRepository candleRepository;

    private HistoricalCoverageChecker checker;

    @BeforeEach
    void setUp() {
        checker = new HistoricalCoverageChecker(candleRepository);
    }

    @Test
    void returnsWholeRangeWhenNoExistingCandles() {
        when(candleRepository.findOpenTimesInRange("BTCUSDT", "1m", FROM, TO)).thenReturn(List.of());

        List<HistoricalRange> missing = checker.findMissingRanges("BTCUSDT", "1m", FROM, TO);

        assertThat(missing).containsExactly(new HistoricalRange(FROM, TO));
    }

    @Test
    void returnsEmptyWhenFullyCovered() {
        List<Instant> existing = List.of(
                FROM, FROM.plus(Duration.ofMinutes(1)), FROM.plus(Duration.ofMinutes(2)),
                FROM.plus(Duration.ofMinutes(3)), FROM.plus(Duration.ofMinutes(4)),
                FROM.plus(Duration.ofMinutes(5)), FROM.plus(Duration.ofMinutes(6)),
                FROM.plus(Duration.ofMinutes(7)), FROM.plus(Duration.ofMinutes(8)),
                FROM.plus(Duration.ofMinutes(9))
        );
        when(candleRepository.findOpenTimesInRange("BTCUSDT", "1m", FROM, TO)).thenReturn(existing);

        assertThat(checker.findMissingRanges("BTCUSDT", "1m", FROM, TO)).isEmpty();
    }

    @Test
    void detectsLeadingGap() {
        Instant firstExisting = FROM.plus(Duration.ofMinutes(3));
        when(candleRepository.findOpenTimesInRange("BTCUSDT", "1m", FROM, TO))
                .thenReturn(List.of(firstExisting, firstExisting.plus(Duration.ofMinutes(1))));

        List<HistoricalRange> missing = checker.findMissingRanges("BTCUSDT", "1m", FROM, TO);

        assertThat(missing).contains(new HistoricalRange(FROM, firstExisting));
    }

    @Test
    void detectsTrailingGap() {
        Instant lastExisting = FROM.plus(Duration.ofMinutes(2));
        when(candleRepository.findOpenTimesInRange("BTCUSDT", "1m", FROM, TO))
                .thenReturn(List.of(FROM, FROM.plus(Duration.ofMinutes(1)), lastExisting));

        List<HistoricalRange> missing = checker.findMissingRanges("BTCUSDT", "1m", FROM, TO);

        assertThat(missing).contains(new HistoricalRange(lastExisting.plus(Duration.ofMinutes(1)), TO));
    }

    @Test
    void detectsInternalGap() {
        Instant beforeGap = FROM.plus(Duration.ofMinutes(2));
        Instant afterGap = FROM.plus(Duration.ofMinutes(5));
        when(candleRepository.findOpenTimesInRange("BTCUSDT", "1m", FROM, TO))
                .thenReturn(List.of(FROM, FROM.plus(Duration.ofMinutes(1)), beforeGap, afterGap, afterGap.plus(Duration.ofMinutes(1))));

        List<HistoricalRange> missing = checker.findMissingRanges("BTCUSDT", "1m", FROM, TO);

        assertThat(missing).contains(new HistoricalRange(beforeGap.plus(Duration.ofMinutes(1)), afterGap));
    }
}
