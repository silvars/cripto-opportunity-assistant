package com.btcassistant.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Insere eventos — nunca atualiza (SDD §21). */
@Component
public class EventPersister {

    private static final Logger log = LoggerFactory.getLogger(EventPersister.class);

    private final MarketEventRepository eventRepository;

    public EventPersister(MarketEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void persist(DetectedEvent event) {
        eventRepository.save(MarketEventEntity.from(event));
        log.info("event_detected symbol={} eventType={} eventTime={} engineVersion={}",
                event.symbol(), event.eventType(), event.eventTime(), event.engineVersion());
    }
}
