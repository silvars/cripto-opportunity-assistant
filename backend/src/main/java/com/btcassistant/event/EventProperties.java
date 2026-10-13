package com.btcassistant.event;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Configuração do Event Detector — versão do motor e thresholds por regra (SDD Fase 2 §5).
 * Estrutura simplificada em relação ao rascunho original (um objeto {@code Rules} plano em vez de
 * um objeto aninhado por regra) — ver docs/phases/FASE-2-event-detector.md §11.
 */
@ConfigurationProperties(prefix = "event-detector")
public class EventProperties {

    private String engineVersion = "1.0";
    private Rules rules = new Rules();

    public String getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    public Rules getRules() {
        return rules;
    }

    public void setRules(Rules rules) {
        this.rules = rules;
    }

    public static class Rules {

        private BigDecimal rapidDropThreshold = new BigDecimal("-0.02");
        private BigDecimal rapidRiseThreshold = new BigDecimal("0.02");
        private BigDecimal volumeSpikeZscore = new BigDecimal("2.5");
        private BigDecimal volatilityExpansionRatio = new BigDecimal("1.5");
        private BigDecimal volatilityContractionRatio = new BigDecimal("0.5");
        private BigDecimal localLowApproachDistance = new BigDecimal("0.01");
        private BigDecimal localHighApproachDistance = new BigDecimal("0.01");
        private BigDecimal recoveryThreshold = new BigDecimal("0.015");
        private BigDecimal failedRecoveryThreshold = new BigDecimal("0.01");
        private BigDecimal failedBreakoutThreshold = new BigDecimal("0.005");

        public BigDecimal getRapidDropThreshold() {
            return rapidDropThreshold;
        }

        public void setRapidDropThreshold(BigDecimal rapidDropThreshold) {
            this.rapidDropThreshold = rapidDropThreshold;
        }

        public BigDecimal getRapidRiseThreshold() {
            return rapidRiseThreshold;
        }

        public void setRapidRiseThreshold(BigDecimal rapidRiseThreshold) {
            this.rapidRiseThreshold = rapidRiseThreshold;
        }

        public BigDecimal getVolumeSpikeZscore() {
            return volumeSpikeZscore;
        }

        public void setVolumeSpikeZscore(BigDecimal volumeSpikeZscore) {
            this.volumeSpikeZscore = volumeSpikeZscore;
        }

        public BigDecimal getVolatilityExpansionRatio() {
            return volatilityExpansionRatio;
        }

        public void setVolatilityExpansionRatio(BigDecimal volatilityExpansionRatio) {
            this.volatilityExpansionRatio = volatilityExpansionRatio;
        }

        public BigDecimal getVolatilityContractionRatio() {
            return volatilityContractionRatio;
        }

        public void setVolatilityContractionRatio(BigDecimal volatilityContractionRatio) {
            this.volatilityContractionRatio = volatilityContractionRatio;
        }

        public BigDecimal getLocalLowApproachDistance() {
            return localLowApproachDistance;
        }

        public void setLocalLowApproachDistance(BigDecimal localLowApproachDistance) {
            this.localLowApproachDistance = localLowApproachDistance;
        }

        public BigDecimal getLocalHighApproachDistance() {
            return localHighApproachDistance;
        }

        public void setLocalHighApproachDistance(BigDecimal localHighApproachDistance) {
            this.localHighApproachDistance = localHighApproachDistance;
        }

        public BigDecimal getRecoveryThreshold() {
            return recoveryThreshold;
        }

        public void setRecoveryThreshold(BigDecimal recoveryThreshold) {
            this.recoveryThreshold = recoveryThreshold;
        }

        public BigDecimal getFailedRecoveryThreshold() {
            return failedRecoveryThreshold;
        }

        public void setFailedRecoveryThreshold(BigDecimal failedRecoveryThreshold) {
            this.failedRecoveryThreshold = failedRecoveryThreshold;
        }

        public BigDecimal getFailedBreakoutThreshold() {
            return failedBreakoutThreshold;
        }

        public void setFailedBreakoutThreshold(BigDecimal failedBreakoutThreshold) {
            this.failedBreakoutThreshold = failedBreakoutThreshold;
        }
    }
}
