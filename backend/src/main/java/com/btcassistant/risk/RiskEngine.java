package com.btcassistant.risk;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.feature.FeatureSnapshot;

/** Classifica o risco/contexto de uma oportunidade (SDD §25). */
public interface RiskEngine {

    RiskAssessment assess(DetectedEvent event, FeatureSnapshot features);
}
