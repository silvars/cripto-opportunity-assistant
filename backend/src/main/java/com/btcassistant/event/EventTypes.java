package com.btcassistant.event;

/** Nomes dos 12 tipos de evento (SDD §20). */
public final class EventTypes {

    public static final String RAPID_DROP = "RAPID_DROP";
    public static final String RAPID_RISE = "RAPID_RISE";
    public static final String VOLUME_SPIKE = "VOLUME_SPIKE";
    public static final String VOLATILITY_EXPANSION = "VOLATILITY_EXPANSION";
    public static final String VOLATILITY_CONTRACTION = "VOLATILITY_CONTRACTION";
    public static final String LOCAL_LOW_APPROACH = "LOCAL_LOW_APPROACH";
    public static final String LOCAL_HIGH_APPROACH = "LOCAL_HIGH_APPROACH";
    public static final String RECOVERY = "RECOVERY";
    public static final String FAILED_RECOVERY = "FAILED_RECOVERY";
    public static final String FAILED_BREAKOUT = "FAILED_BREAKOUT";
    public static final String BREAKOUT = "BREAKOUT";
    public static final String BREAKDOWN = "BREAKDOWN";

    private EventTypes() {
    }
}
