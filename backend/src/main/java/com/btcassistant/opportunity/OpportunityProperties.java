package com.btcassistant.opportunity;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/** Thresholds, pesos de score e escalas de normalização — config-driven (SDD §23/§24, Fase 3 §5). */
@ConfigurationProperties(prefix = "opportunity")
public class OpportunityProperties {

    private BigDecimal watchThreshold = new BigDecimal("50");
    private BigDecimal setupThreshold = new BigDecimal("65");
    private BigDecimal strongSetupThreshold = new BigDecimal("80");
    private String configVersion = "1.0";
    private Scoring scoring = new Scoring();
    private EventStrengthScales eventStrengthScales = new EventStrengthScales();

    public BigDecimal getWatchThreshold() {
        return watchThreshold;
    }

    public void setWatchThreshold(BigDecimal watchThreshold) {
        this.watchThreshold = watchThreshold;
    }

    public BigDecimal getSetupThreshold() {
        return setupThreshold;
    }

    public void setSetupThreshold(BigDecimal setupThreshold) {
        this.setupThreshold = setupThreshold;
    }

    public BigDecimal getStrongSetupThreshold() {
        return strongSetupThreshold;
    }

    public void setStrongSetupThreshold(BigDecimal strongSetupThreshold) {
        this.strongSetupThreshold = strongSetupThreshold;
    }

    public String getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(String configVersion) {
        this.configVersion = configVersion;
    }

    public Scoring getScoring() {
        return scoring;
    }

    public void setScoring(Scoring scoring) {
        this.scoring = scoring;
    }

    public EventStrengthScales getEventStrengthScales() {
        return eventStrengthScales;
    }

    public void setEventStrengthScales(EventStrengthScales eventStrengthScales) {
        this.eventStrengthScales = eventStrengthScales;
    }

    /** Pesos dos componentes do score (SDD §24) — não precisam somar 100; a fórmula renormaliza sobre o peso disponível. */
    public static class Scoring {

        private BigDecimal eventStrength = new BigDecimal("20");
        private BigDecimal historicalSupport = new BigDecimal("30");
        private BigDecimal momentumContext = new BigDecimal("15");
        private BigDecimal volumeConfirmation = new BigDecimal("15");
        private BigDecimal volatilityContext = new BigDecimal("10");
        private BigDecimal riskPenalty = new BigDecimal("20");

        public BigDecimal getEventStrength() {
            return eventStrength;
        }

        public void setEventStrength(BigDecimal eventStrength) {
            this.eventStrength = eventStrength;
        }

        public BigDecimal getHistoricalSupport() {
            return historicalSupport;
        }

        public void setHistoricalSupport(BigDecimal historicalSupport) {
            this.historicalSupport = historicalSupport;
        }

        public BigDecimal getMomentumContext() {
            return momentumContext;
        }

        public void setMomentumContext(BigDecimal momentumContext) {
            this.momentumContext = momentumContext;
        }

        public BigDecimal getVolumeConfirmation() {
            return volumeConfirmation;
        }

        public void setVolumeConfirmation(BigDecimal volumeConfirmation) {
            this.volumeConfirmation = volumeConfirmation;
        }

        public BigDecimal getVolatilityContext() {
            return volatilityContext;
        }

        public void setVolatilityContext(BigDecimal volatilityContext) {
            this.volatilityContext = volatilityContext;
        }

        public BigDecimal getRiskPenalty() {
            return riskPenalty;
        }

        public void setRiskPenalty(BigDecimal riskPenalty) {
            this.riskPenalty = riskPenalty;
        }
    }

    /** Escalas de normalização de magnitude por "família" de evento (SDD Fase 3 §11). */
    public static class EventStrengthScales {

        private BigDecimal shockMagnitude = new BigDecimal("0.03");
        private BigDecimal proximityMagnitude = new BigDecimal("0.01");
        private BigDecimal volumeSpikeMagnitude = new BigDecimal("5.0");
        private BigDecimal volatilityExpansionMagnitude = new BigDecimal("3.0");
        private BigDecimal volatilityContractionMagnitude = new BigDecimal("0.5");
        private BigDecimal momentumContext = new BigDecimal("0.02");
        private BigDecimal volumeConfirmation = new BigDecimal("3.0");
        private BigDecimal volatilityContext = new BigDecimal("2.0");
        private BigDecimal riskPenalty = new BigDecimal("0.05");

        public BigDecimal getShockMagnitude() {
            return shockMagnitude;
        }

        public void setShockMagnitude(BigDecimal shockMagnitude) {
            this.shockMagnitude = shockMagnitude;
        }

        public BigDecimal getProximityMagnitude() {
            return proximityMagnitude;
        }

        public void setProximityMagnitude(BigDecimal proximityMagnitude) {
            this.proximityMagnitude = proximityMagnitude;
        }

        public BigDecimal getVolumeSpikeMagnitude() {
            return volumeSpikeMagnitude;
        }

        public void setVolumeSpikeMagnitude(BigDecimal volumeSpikeMagnitude) {
            this.volumeSpikeMagnitude = volumeSpikeMagnitude;
        }

        public BigDecimal getVolatilityExpansionMagnitude() {
            return volatilityExpansionMagnitude;
        }

        public void setVolatilityExpansionMagnitude(BigDecimal volatilityExpansionMagnitude) {
            this.volatilityExpansionMagnitude = volatilityExpansionMagnitude;
        }

        public BigDecimal getVolatilityContractionMagnitude() {
            return volatilityContractionMagnitude;
        }

        public void setVolatilityContractionMagnitude(BigDecimal volatilityContractionMagnitude) {
            this.volatilityContractionMagnitude = volatilityContractionMagnitude;
        }

        public BigDecimal getMomentumContext() {
            return momentumContext;
        }

        public void setMomentumContext(BigDecimal momentumContext) {
            this.momentumContext = momentumContext;
        }

        public BigDecimal getVolumeConfirmation() {
            return volumeConfirmation;
        }

        public void setVolumeConfirmation(BigDecimal volumeConfirmation) {
            this.volumeConfirmation = volumeConfirmation;
        }

        public BigDecimal getVolatilityContext() {
            return volatilityContext;
        }

        public void setVolatilityContext(BigDecimal volatilityContext) {
            this.volatilityContext = volatilityContext;
        }

        public BigDecimal getRiskPenalty() {
            return riskPenalty;
        }

        public void setRiskPenalty(BigDecimal riskPenalty) {
            this.riskPenalty = riskPenalty;
        }
    }
}
