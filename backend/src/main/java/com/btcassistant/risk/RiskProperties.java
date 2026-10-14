package com.btcassistant.risk;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Pesos e escalas do Risk Engine — config-driven, nunca hardcoded (SDD §76 / .github/copilot-instructions.md).
 * Ver docs/phases/FASE-3-opportunity-risk.md §11 para a justificativa da fórmula composta.
 */
@ConfigurationProperties(prefix = "risk")
public class RiskProperties {

    private BigDecimal highVolatilityScale = new BigDecimal("0.05");
    private BigDecimal proximityScale = new BigDecimal("0.01");
    private BigDecimal magnitudeScale = new BigDecimal("1.0");
    private BigDecimal volatilityWeight = new BigDecimal("0.5");
    private BigDecimal proximityWeight = new BigDecimal("0.3");
    private BigDecimal magnitudeWeight = new BigDecimal("0.2");
    private BigDecimal mediumRiskThreshold = new BigDecimal("0.33");
    private BigDecimal highRiskThreshold = new BigDecimal("0.66");
    private BigDecimal significantComponentThreshold = new BigDecimal("0.5");

    public BigDecimal getHighVolatilityScale() {
        return highVolatilityScale;
    }

    public void setHighVolatilityScale(BigDecimal highVolatilityScale) {
        this.highVolatilityScale = highVolatilityScale;
    }

    public BigDecimal getProximityScale() {
        return proximityScale;
    }

    public void setProximityScale(BigDecimal proximityScale) {
        this.proximityScale = proximityScale;
    }

    public BigDecimal getMagnitudeScale() {
        return magnitudeScale;
    }

    public void setMagnitudeScale(BigDecimal magnitudeScale) {
        this.magnitudeScale = magnitudeScale;
    }

    public BigDecimal getVolatilityWeight() {
        return volatilityWeight;
    }

    public void setVolatilityWeight(BigDecimal volatilityWeight) {
        this.volatilityWeight = volatilityWeight;
    }

    public BigDecimal getProximityWeight() {
        return proximityWeight;
    }

    public void setProximityWeight(BigDecimal proximityWeight) {
        this.proximityWeight = proximityWeight;
    }

    public BigDecimal getMagnitudeWeight() {
        return magnitudeWeight;
    }

    public void setMagnitudeWeight(BigDecimal magnitudeWeight) {
        this.magnitudeWeight = magnitudeWeight;
    }

    public BigDecimal getMediumRiskThreshold() {
        return mediumRiskThreshold;
    }

    public void setMediumRiskThreshold(BigDecimal mediumRiskThreshold) {
        this.mediumRiskThreshold = mediumRiskThreshold;
    }

    public BigDecimal getHighRiskThreshold() {
        return highRiskThreshold;
    }

    public void setHighRiskThreshold(BigDecimal highRiskThreshold) {
        this.highRiskThreshold = highRiskThreshold;
    }

    public BigDecimal getSignificantComponentThreshold() {
        return significantComponentThreshold;
    }

    public void setSignificantComponentThreshold(BigDecimal significantComponentThreshold) {
        this.significantComponentThreshold = significantComponentThreshold;
    }
}
