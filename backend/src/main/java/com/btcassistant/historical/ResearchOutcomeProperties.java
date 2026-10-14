package com.btcassistant.historical;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/** Metodologia de target/stop — versionada separadamente de {@code historicalVersion} (SDD §30). */
@ConfigurationProperties(prefix = "research.outcome")
public class ResearchOutcomeProperties {

    private String targetDefinitionVersion = "1.0";
    private String stopDefinitionVersion = "1.0";
    private BigDecimal targetPercentage = new BigDecimal("0.01");
    private BigDecimal stopPercentage = new BigDecimal("0.01");

    public String getTargetDefinitionVersion() {
        return targetDefinitionVersion;
    }

    public void setTargetDefinitionVersion(String targetDefinitionVersion) {
        this.targetDefinitionVersion = targetDefinitionVersion;
    }

    public String getStopDefinitionVersion() {
        return stopDefinitionVersion;
    }

    public void setStopDefinitionVersion(String stopDefinitionVersion) {
        this.stopDefinitionVersion = stopDefinitionVersion;
    }

    public BigDecimal getTargetPercentage() {
        return targetPercentage;
    }

    public void setTargetPercentage(BigDecimal targetPercentage) {
        this.targetPercentage = targetPercentage;
    }

    public BigDecimal getStopPercentage() {
        return stopPercentage;
    }

    public void setStopPercentage(BigDecimal stopPercentage) {
        this.stopPercentage = stopPercentage;
    }
}
