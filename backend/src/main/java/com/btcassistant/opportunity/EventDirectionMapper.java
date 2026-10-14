package com.btcassistant.opportunity;

import com.btcassistant.event.EventTypes;
import org.springframework.stereotype.Component;

/**
 * Mapeia o tipo de evento para uma direção implícita (SDD §22 — "Não executa operações", apenas
 * classifica). Eventos de contexto/volatilidade (volume, volatilidade, aproximação de extremos)
 * não implicam direção por si só — ver docs/phases/FASE-3-opportunity-risk.md §11.
 */
@Component
public class EventDirectionMapper {

    public Direction map(String eventType) {
        return switch (eventType) {
            case EventTypes.RAPID_RISE, EventTypes.RECOVERY, EventTypes.BREAKOUT -> Direction.BUY;
            case EventTypes.RAPID_DROP, EventTypes.BREAKDOWN, EventTypes.FAILED_RECOVERY, EventTypes.FAILED_BREAKOUT ->
                    Direction.SELL;
            default -> Direction.NEUTRAL;
        };
    }
}
