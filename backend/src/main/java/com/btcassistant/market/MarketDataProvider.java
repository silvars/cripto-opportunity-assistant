package com.btcassistant.market;

import java.time.Instant;
import java.util.List;

/** Abstração de fonte de mercado (SDD §12) — implementação inicial: BinanceMarketDataProvider. */
public interface MarketDataProvider {

    void streamLive(Instrument instrument, CandleListener listener);

    List<Candle> fetchHistorical(Instrument instrument, Instant from, Instant to);
}
