package com.btcassistant.historical.web;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.event.MarketEventEntity;
import com.btcassistant.event.MarketEventRepository;
import com.btcassistant.historical.HistoricalAnalyzer;
import com.btcassistant.historical.OutcomeStats;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Recalcula o contexto histórico sob demanda — útil para auditoria/dashboard (não cacheado). */
@RestController
@RequestMapping("/api/v1/historical")
public class HistoricalController {

    private final MarketEventRepository eventRepository;
    private final HistoricalAnalyzer historicalAnalyzer;

    public HistoricalController(MarketEventRepository eventRepository, HistoricalAnalyzer historicalAnalyzer) {
        this.eventRepository = eventRepository;
        this.historicalAnalyzer = historicalAnalyzer;
    }

    @GetMapping("/events/{eventId}")
    public HistoricalContextResponse similarTo(@PathVariable UUID eventId) {
        MarketEventEntity entity = eventRepository.findById(eventId).orElse(null);
        if (entity == null) {
            return null;
        }

        DetectedEvent detectedEvent = new DetectedEvent(entity.getSymbol(), entity.getEventType(), entity.getEventTime(),
                entity.getPrice(), entity.getFeaturesSnapshot(), entity.getMagnitude(),
                entity.getDurationSeconds() == null ? null : java.time.Duration.ofSeconds(entity.getDurationSeconds()),
                entity.getEngineVersion());

        var context = historicalAnalyzer.findSimilar(detectedEvent, detectedEvent.eventTime());

        List<HistoricalContextResponse.SimilarEventResponse> similarEvents = context.similarEvents().stream()
                .map(s -> new HistoricalContextResponse.SimilarEventResponse(s.eventId(), s.eventTime(), s.similarityScore()))
                .toList();
        Map<String, OutcomeStatsResponse> outcomes = context.outcomesByHorizon().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> toResponse(e.getValue())));

        return new HistoricalContextResponse(context.sampleSize(), similarEvents, outcomes, context.historicalVersion());
    }

    private static OutcomeStatsResponse toResponse(OutcomeStats stats) {
        return new OutcomeStatsResponse(stats.targetBeforeStopProbability(), stats.medianTimeToTarget(), stats.medianAdverseMove());
    }
}
