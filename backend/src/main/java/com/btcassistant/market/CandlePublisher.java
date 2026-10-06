package com.btcassistant.market;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Publica candles fechados para os clientes WebSocket do cockpit (SDD §51). */
@Component
public class CandlePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public CandlePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(Candle candle) {
        messagingTemplate.convertAndSend("/topic/market/" + candle.symbol(), candle);
    }

    public void publishStatus(MarketStatus status) {
        messagingTemplate.convertAndSend("/topic/market-status/" + status.symbol(), status);
    }
}
