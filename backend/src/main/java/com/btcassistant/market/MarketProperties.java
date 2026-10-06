package com.btcassistant.market;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Instrumentos são configuração, nunca hardcoded (SDD §9, ADR-008). */
@ConfigurationProperties(prefix = "market")
public class MarketProperties {

    private List<InstrumentConfig> instruments = List.of();

    public List<InstrumentConfig> getInstruments() {
        return instruments;
    }

    public void setInstruments(List<InstrumentConfig> instruments) {
        this.instruments = instruments;
    }

    public List<Instrument> enabledInstruments() {
        return instruments.stream()
                .filter(InstrumentConfig::isEnabled)
                .map(InstrumentConfig::toInstrument)
                .toList();
    }

    public Instrument findEnabled(String symbol, String interval) {
        return enabledInstruments().stream()
                .filter(i -> i.symbol().equals(symbol) && i.interval().equals(interval))
                .findFirst()
                .orElse(null);
    }

    public static class InstrumentConfig {

        private String symbol;
        private String baseAsset;
        private String quoteAsset;
        private String exchange;
        private String interval;
        private boolean enabled;
        private HistoricalBackfillConfig historicalBackfill = new HistoricalBackfillConfig();

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getBaseAsset() {
            return baseAsset;
        }

        public void setBaseAsset(String baseAsset) {
            this.baseAsset = baseAsset;
        }

        public String getQuoteAsset() {
            return quoteAsset;
        }

        public void setQuoteAsset(String quoteAsset) {
            this.quoteAsset = quoteAsset;
        }

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        public String getInterval() {
            return interval;
        }

        public void setInterval(String interval) {
            this.interval = interval;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public HistoricalBackfillConfig getHistoricalBackfill() {
            return historicalBackfill;
        }

        public void setHistoricalBackfill(HistoricalBackfillConfig historicalBackfill) {
            this.historicalBackfill = historicalBackfill;
        }

        public Instrument toInstrument() {
            return new Instrument(symbol, baseAsset, quoteAsset, exchange, interval, enabled);
        }
    }

    public static class HistoricalBackfillConfig {

        private boolean enabled = true;
        private String period = "12-months";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPeriod() {
            return period;
        }

        public void setPeriod(String period) {
            this.period = period;
        }
    }
}
