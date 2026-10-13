package com.btcassistant.event.web;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.event.EventTypes;
import com.btcassistant.event.MarketEventEntity;
import com.btcassistant.event.MarketEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private MarketEventRepository eventRepository;

    @Test
    void returnsMappedRecentEvents() {
        DetectedEvent event = new DetectedEvent("BTCUSDT", EventTypes.RAPID_DROP, Instant.parse("2026-01-01T00:00:00Z"),
                new BigDecimal("100"), Map.of("return_15m", new BigDecimal("-0.03")), new BigDecimal("0.03"),
                Duration.ofMinutes(15), "1.0");
        when(eventRepository.findTop50BySymbolOrderByEventTimeDesc("BTCUSDT"))
                .thenReturn(List.of(MarketEventEntity.from(event)));

        List<EventResponse> responses = new EventController(eventRepository).recent("BTCUSDT");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).eventType()).isEqualTo(EventTypes.RAPID_DROP);
        assertThat(responses.get(0).magnitude()).isEqualByComparingTo("0.03");
        assertThat(responses.get(0).durationSeconds()).isEqualTo(900L);
    }

    @Test
    void returnsEmptyListWhenNoEvents() {
        when(eventRepository.findTop50BySymbolOrderByEventTimeDesc("BTCUSDT")).thenReturn(List.of());

        assertThat(new EventController(eventRepository).recent("BTCUSDT")).isEmpty();
    }
}
