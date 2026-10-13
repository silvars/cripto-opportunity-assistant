package com.btcassistant.feature;

import java.time.Instant;

/** Publicado após um {@link FeatureSnapshot} novo ser persistido (nunca em recálculo redundante). */
public record FeatureCalculatedEvent(String symbol, String interval, Instant candleOpenTime, String featureVersion) {
}
