package com.btcassistant.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventPersisterTest {

    @Mock
    private MarketEventRepository eventRepository;

    @Test
    void savesEntityBuiltFromDetectedEventAndReturnsGeneratedId() {
        EventPersister persister = new EventPersister(eventRepository);
        DetectedEvent event = new DetectedEvent("BTCUSDT", EventTypes.RAPID_DROP, Instant.parse("2026-01-01T00:00:00Z"),
                new BigDecimal("100"), Map.of(), new BigDecimal("0.03"), Duration.ofMinutes(15), "1.0");
        when(eventRepository.save(any(MarketEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        java.util.UUID eventId = persister.persist(event);

        assertThat(eventId).isNotNull();
        verify(eventRepository).save(any(MarketEventEntity.class));
    }
}
