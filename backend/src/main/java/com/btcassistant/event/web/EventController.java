package com.btcassistant.event.web;

import com.btcassistant.event.MarketEventEntity;
import com.btcassistant.event.MarketEventRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final MarketEventRepository eventRepository;

    public EventController(MarketEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GetMapping("/{symbol}")
    public List<EventResponse> recent(@PathVariable String symbol) {
        return eventRepository.findTop50BySymbolOrderByEventTimeDesc(symbol).stream()
                .map(EventController::toResponse)
                .toList();
    }

    private static EventResponse toResponse(MarketEventEntity e) {
        return new EventResponse(e.getEventId(), e.getSymbol(), e.getEventType(), e.getEventTime(), e.getPrice(),
                e.getFeaturesSnapshot(), e.getMagnitude(), e.getDurationSeconds(), e.getEngineVersion());
    }
}
