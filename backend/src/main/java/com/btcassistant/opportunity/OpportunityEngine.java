package com.btcassistant.opportunity;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.feature.FeatureSnapshot;

import java.util.Optional;

/** Transforma um evento + contexto quantitativo em uma classificação explicável (SDD §22). */
public interface OpportunityEngine {

    OpportunityResult evaluate(DetectedEvent event, FeatureSnapshot features, Optional<HistoricalContext> historicalContext);
}
