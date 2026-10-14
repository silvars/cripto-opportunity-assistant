package com.btcassistant.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Insere eventos — nunca atualiza (SDD §21). */
@Component
public class EventPersister {

    private static final Logger log = LoggerFactory.getLogger(EventPersister.class);

    private final MarketEventRepository eventRepository;

    public EventPersister(MarketEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /** @return o {@code eventId} gerado, usado pelo chamador para publicar {@link MarketEventPersistedEvent}. */
    public UUID persist(DetectedEvent event) {
        MarketEventEntity entity = eventRepository.save(MarketEventEntity.from(event));
        log.info("event_detected symbol={} eventType={} eventTime={} engineVersion={}",
                event.symbol(), event.eventType(), event.eventTime(), event.engineVersion());
        return entity.getEventId();
    }
}
