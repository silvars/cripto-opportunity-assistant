package com.btcassistant.historical;

import com.btcassistant.event.MarketEventEntity;
import com.btcassistant.event.MarketEventRepository;
import com.btcassistant.market.Instrument;
import com.btcassistant.market.MarketProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricalOutcomeBackfillServiceTest {

    private static final Instrument INSTRUMENT = new Instrument("BTCUSDT", "BTC", "USDT", "BINANCE", "1m", true);

    @Mock
    private MarketEventRepository eventRepository;
    @Mock
    private HistoricalOutcomeRepository outcomeRepository;
    @Mock
    private HistoricalOutcomeCalculator calculator;
    @Mock
    private MarketProperties marketProperties;

    private HistoricalOutcomeBackfillService service() {
        return new HistoricalOutcomeBackfillService(eventRepository, outcomeRepository, calculator, marketProperties);
    }

    private static MarketEventEntity eventEntity() {
        return MarketEventEntity.from(new com.btcassistant.event.DetectedEvent("BTCUSDT",
                com.btcassistant.event.EventTypes.RAPID_DROP, Instant.parse("2026-01-01T00:00:00Z"),
                new BigDecimal("100"), Map.of(), new BigDecimal("0.05"), Duration.ofMinutes(15), "1.0"));
    }

    @Test
    void skipsEventsWithoutAnEnabledInstrument() {
        when(eventRepository.findAll()).thenReturn(List.of(eventEntity()));
        when(marketProperties.findEnabledBySymbol("BTCUSDT")).thenReturn(null);

        service().computeMissingOutcomes();

        verify(calculator, never()).calculate(any(), any(), any(), any(), any(), any());
    }

    @Test
    void skipsHorizonsAlreadyComputed() {
        MarketEventEntity event = eventEntity();
        when(eventRepository.findAll()).thenReturn(List.of(event));
        when(marketProperties.findEnabledBySymbol("BTCUSDT")).thenReturn(INSTRUMENT);
        HistoricalOutcomeResult existing = new HistoricalOutcomeResult(event.getEventId(), "5m", new BigDecimal("0.01"),
                new BigDecimal("0.02"), new BigDecimal("-0.01"), Duration.ofMinutes(2), null, true,
                event.getEventTime().plus(Duration.ofMinutes(5)), "1.0", "1.0");
        when(outcomeRepository.findByEventIdAndHorizon(eq(event.getEventId()), any()))
                .thenReturn(Optional.of(HistoricalOutcomeEntity.from(existing)));

        service().computeMissingOutcomes();

        verify(calculator, never()).calculate(any(), any(), any(), any(), any(), any());
    }

    @Test
    void computesAndPersistsOutcomesNotYetComputedButNowAvailable() {
        MarketEventEntity event = eventEntity();
        when(eventRepository.findAll()).thenReturn(List.of(event));
        when(marketProperties.findEnabledBySymbol("BTCUSDT")).thenReturn(INSTRUMENT);
        when(outcomeRepository.findByEventIdAndHorizon(eq(event.getEventId()), any())).thenReturn(Optional.empty());
        when(calculator.calculate(any(), any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        when(calculator.calculate(eq(event.getEventId()), eq("BTCUSDT"), eq("1m"), eq(event.getEventTime()), eq(event.getPrice()), eq("5m")))
                .thenReturn(Optional.of(new HistoricalOutcomeResult(event.getEventId(), "5m", new BigDecimal("0.01"),
                        new BigDecimal("0.02"), new BigDecimal("-0.01"), Duration.ofMinutes(2), null, true,
                        event.getEventTime().plus(Duration.ofMinutes(5)), "1.0", "1.0")));

        service().computeMissingOutcomes();

        verify(outcomeRepository, times(1)).save(any(HistoricalOutcomeEntity.class));
    }
}
