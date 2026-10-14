package com.btcassistant.alert;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.historical.HistoricalContext;

import java.util.Optional;

/** Publicado apenas quando um alerta NOVO é de fato criado — nunca para ocorrências deduplicadas (SDD Fase 5 §6). */
public record AlertCreatedEvent(AlertEntity alert, DetectedEvent event, Optional<HistoricalContext> historicalContext) {
}
