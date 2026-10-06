package com.btcassistant.market;

/** Estado do módulo market por instrumento (SDD Fase 0 — Initial Historical Backfill). */
public enum MarketPhase {
    INITIALIZING,
    CHECKING_HISTORY,
    BACKFILLING,
    RETRYING,
    HISTORICAL_DATA_READY,
    LIVE
}
