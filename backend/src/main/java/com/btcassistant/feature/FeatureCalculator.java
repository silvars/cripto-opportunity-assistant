package com.btcassistant.feature;

import com.btcassistant.market.Instrument;

import java.time.Instant;

/** Contrato do orquestrador do Feature Engine (SDD Fase 1 §4). */
public interface FeatureCalculator {

    String featureVersion();

    /** @return o snapshot calculado em {@code atCandleOpenTime}, ou {@code null} se não houver candle persistido nesse instante. */
    FeatureSnapshot calculate(Instrument instrument, Instant atCandleOpenTime);
}
