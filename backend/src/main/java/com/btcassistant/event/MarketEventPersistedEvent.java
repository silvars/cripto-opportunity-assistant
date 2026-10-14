package com.btcassistant.event;

import java.util.UUID;

/** Publicado após um {@link DetectedEvent} novo ser persistido em {@code market_events}. */
public record MarketEventPersistedEvent(UUID eventId, DetectedEvent event) {
}
